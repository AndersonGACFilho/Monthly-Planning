package com.planejamentomensal.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class ApiNamingIT {

    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*|\\{[a-zA-Z]+}");
    private static final Set<String> VERBS = Set.of("get", "list", "create", "add", "update", "edit", "delete",
            "remove", "save", "find", "search", "do", "execute", "run", "fetch", "set");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    @Test
    @DisplayName("API-01: API paths are kebab-case nouns under /api, without verbs")
    void apiPathsFollowNamingRules() {
        // Given every mapped API path
        List<String> paths = mapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream())
                .filter(path -> path.startsWith("/api"))
                .toList();
        // When / Then
        assertThat(paths).isNotEmpty().allSatisfy(path -> {
            for (String segment : path.substring(1).split("/")) {
                assertThat(segment).as("segment of %s", path).matches(SEGMENT);
                for (String word : segment.split("-")) {
                    assertThat(VERBS).as("verb in %s", path).doesNotContain(word);
                }
            }
        });
    }
}
