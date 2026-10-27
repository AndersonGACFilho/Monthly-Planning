package com.planejamentomensal.alias.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AliasTest {

    @Test
    @DisplayName("RF-46, RF-50: override wins over alias, alias over the original")
    void resolution() {
        assertThat(DisplayDescriptionResolver.resolve("Só este", "Mercado do bairro", "SUPERMERC JOAO LTDA")).isEqualTo("Só este");
        assertThat(DisplayDescriptionResolver.resolve(null, "Mercado do bairro", "SUPERMERC JOAO LTDA")).isEqualTo("Mercado do bairro");
        assertThat(DisplayDescriptionResolver.resolve(" ", null, "SUPERMERC JOAO LTDA")).isEqualTo("SUPERMERC JOAO LTDA");
    }

    @Test
    @DisplayName("RF-47, RN-24: renaming replaces the name and keeps the patterns; an alias groups several patterns")
    void renameAndGroup() {
        Alias alias = new Alias("Paramount", "ECPARAMOUNTPLUSOS", 4L);
        alias.rename("  Paramount+  ");
        alias.addPattern("MPPARAMOUNTPLUSOSASCO");
        assertThat(alias.displayName()).isEqualTo("Paramount+");
        assertThat(alias.patterns()).containsExactlyInAnyOrder("ECPARAMOUNTPLUSOS", "MPPARAMOUNTPLUSOSASCO");
        assertThat(alias.defaultCategoryId()).isEqualTo(4L);
        alias.defaultCategory(null);
        assertThat(alias.removePattern("ECPARAMOUNTPLUSOS")).isFalse();
        assertThat(alias.removePattern("MPPARAMOUNTPLUSOSASCO")).isTrue();
        assertThatThrownBy(() -> alias.rename(" ")).isInstanceOf(IllegalArgumentException.class);
        AliasOverride override = new AliasOverride(7, "Presente");
        override.rename("Presente da tia");
        assertThat(override.displayName()).isEqualTo("Presente da tia");
        assertThat(override.billEntryId()).isEqualTo(7);
        assertThatThrownBy(() -> new AliasOverride(1, "")).isInstanceOf(IllegalArgumentException.class);
    }
}
