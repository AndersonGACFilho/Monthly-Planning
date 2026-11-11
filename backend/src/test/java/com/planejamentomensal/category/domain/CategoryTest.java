package com.planejamentomensal.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    @DisplayName("RF-06: editing changes name, parent, colour and icon")
    void updates() {
        Category category = new Category(new CategoryDetails("Mercado", null, "#000000", "cart", false), false);
        category.update(new CategoryDetails("Feira", 3L, "#FFFFFF", "apple", false));
        assertThat(category.name()).isEqualTo("Feira");
        assertThat(category.parentId()).isEqualTo(3L);
        assertThat(category.color()).isEqualTo("#FFFFFF");
        assertThat(category.icon()).isEqualTo("apple");
        assertThat(category.system()).isFalse();
        assertThat(category.version()).isZero();
    }

    @Test
    @DisplayName("IMP-65: a category is marked as subscription and unmarked")
    void subscription() {
        Category category = new Category(new CategoryDetails("Streaming", null, "#6366F1", "repeat", true), false);
        assertThat(category.subscription()).isTrue();
        category.update(new CategoryDetails("Streaming", null, "#6366F1", "repeat", false));
        assertThat(category.subscription()).isFalse();
    }

    @Test
    @DisplayName("RF-07, IMP-65: the system category never gets a parent nor becomes a subscription")
    void systemCategoryHasNoParent() {
        Category unclassified = new Category(new CategoryDetails("Não classificado", null, "#9CA3AF", "circle-help", false), true);
        unclassified.update(new CategoryDetails("Sem categoria", 3L, "#9CA3AF", "circle-help", true));
        assertThat(unclassified.parentId()).isNull();
        assertThat(unclassified.subscription()).isFalse();
        assertThat(unclassified.system()).isTrue();
        assertThat(unclassified.id()).isNull();
    }
}
