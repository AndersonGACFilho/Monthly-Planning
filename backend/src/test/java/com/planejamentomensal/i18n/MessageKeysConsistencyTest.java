package com.planejamentomensal.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageKeysConsistencyTest {

    private static Set<String> keys(String bundle) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MessageKeysConsistencyTest.class.getResourceAsStream("/i18n/" + bundle)) {
            assertThat(in).as(bundle).isNotNull();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties.stringPropertyNames();
    }

    @Test
    @DisplayName("I18N-13: pt-BR, en-US and fallback bundles have the same keys")
    void bundlesHaveSameKeys() throws IOException {
        // Given
        Set<String> fallback = keys("messages.properties");
        // When / Then
        assertThat(keys("messages_pt_BR.properties")).containsExactlyInAnyOrderElementsOf(fallback);
        assertThat(keys("messages_en_US.properties")).containsExactlyInAnyOrderElementsOf(fallback);
    }
}
