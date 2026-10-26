package com.planejamentomensal.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    @DisplayName("RF-06: editing changes name, parent, colour and icon")
    void updates() {
        Category category = new Category("Mercado", null, "#000000", "cart", false);
        category.update("Feira", 3L, "#FFFFFF", "apple");
        assertThat(category.name()).isEqualTo("Feira");
        assertThat(category.parentId()).isEqualTo(3L);
        assertThat(category.color()).isEqualTo("#FFFFFF");
        assertThat(category.icon()).isEqualTo("apple");
        assertThat(category.system()).isFalse();
        assertThat(category.version()).isZero();
    }

    @Test
    @DisplayName("RF-07: the system category never gets a parent")
    void systemCategoryHasNoParent() {
        Category unclassified = new Category("Não classificado", null, "#9CA3AF", "circle-help", true);
        unclassified.update("Sem categoria", 3L, "#9CA3AF", "circle-help");
        assertThat(unclassified.parentId()).isNull();
        assertThat(unclassified.system()).isTrue();
        assertThat(unclassified.id()).isNull();
    }
}
