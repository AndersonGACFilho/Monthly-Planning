package com.planejamentomensal.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.StaleVersionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ETagsTest {

    @Test
    @DisplayName("API-04: version becomes a strong entity tag")
    void formatsTag() {
        assertThat(ETags.of(3)).isEqualTo("\"3\"");
        assertThat(ETags.ok("x", 7).getHeaders().getETag()).isEqualTo("\"7\"");
    }

    @Test
    @DisplayName("API-04: matching or missing If-Match passes, another version fails")
    void checksIfMatch() {
        assertThatCode(() -> ETags.checkIfMatch("\"2\"", 2)).doesNotThrowAnyException();
        assertThatCode(() -> ETags.checkIfMatch("W/\"2\"", 2)).doesNotThrowAnyException();
        assertThatCode(() -> ETags.checkIfMatch(null, 2)).doesNotThrowAnyException();
        assertThatCode(() -> ETags.checkIfMatch("*", 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ETags.checkIfMatch("\"1\"", 2)).isInstanceOf(StaleVersionException.class);
        assertThatThrownBy(() -> ETags.checkIfMatch("\"abc\"", 2)).isInstanceOf(StaleVersionException.class);
    }
}
