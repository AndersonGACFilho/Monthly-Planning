package com.planejamentomensal.shared.api.openapi;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Keeps the committed OpenAPI contract ({@code frontend/openapi/openapi.json}) equal to the running API.
 *
 * <p>After an intended API change, refresh it with {@code ./mvnw test -Dtest=OpenApiContractIT
 * -Dopenapi.update=true} and regenerate the frontend client.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractIT {

    @Autowired
    private MockMvc mockMvc;

    private String currentContract() throws Exception {
        String json = mockMvc.perform(authorized(get("/v3/api-docs"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.strip() + "\n";
    }

    @Test
    @DisplayName("API-12: committed OpenAPI contract matches the running API")
    void committedContractMatches() throws Exception {
        // Given
        Path committed = Path.of(System.getProperty("openapi.contract"));
        String current = currentContract();
        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(committed.getParent());
            Files.writeString(committed, current, StandardCharsets.UTF_8);
        }
        // When / Then
        assertThat(committed).exists();
        assertThat(Files.readString(committed, StandardCharsets.UTF_8))
                .as("OpenAPI changed: run ./mvnw test -Dtest=OpenApiContractIT -Dopenapi.update=true")
                .isEqualTo(current);
    }

    @Test
    @DisplayName("DOC-05: operation descriptions come from controller Javadoc")
    void descriptionsComeFromJavadoc() throws Exception {
        // Given / When
        String contract = currentContract();
        // Then
        assertThat(contract)
                .contains("Returns links to every top-level resource and to the HAL profile.")
                .contains("X-Session-Token")
                .contains("\"/api/profile/{name}\"");
    }
}
