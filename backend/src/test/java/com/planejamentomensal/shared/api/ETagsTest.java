package com.planejamentomensal.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.StaleVersionException;
import com.planejamentomensal.shared.domain.Versions;
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
    @DisplayName("API-04: If-Match is parsed into the expected version")
    void parsesIfMatch() {
        assertThat(ETags.expectedVersion("\"2\"")).isEqualTo(2L);
        assertThat(ETags.expectedVersion("W/\"2\"")).isEqualTo(2L);
        assertThat(ETags.expectedVersion(null)).isNull();
        assertThat(ETags.expectedVersion("*")).isNull();
        assertThatThrownBy(() -> ETags.expectedVersion("\"abc\"")).isInstanceOf(StaleVersionException.class);
    }

    @Test
    @DisplayName("API-04: matching or missing version passes, another version fails")
    void checksVersion() {
        assertThatCode(() -> Versions.check(2L, 2)).doesNotThrowAnyException();
        assertThatCode(() -> Versions.check(null, 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Versions.check(1L, 2)).isInstanceOf(StaleVersionException.class);
    }
}
