package com.planejamentomensal.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.category.domain.Category;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.StaleVersionException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(CategoryUseCasesIT.References.class)
class CategoryUseCasesIT {

    static final Set<Long> REFERENCED = new HashSet<>();

    @TestConfiguration
    static class References {
        @Bean
        CategoryReferences testCategoryReferences() {
            return REFERENCED::contains;
        }
    }

    @Autowired
    private SaveCategoryUseCase save;

    @Autowired
    private CategoryQueries queries;

    @BeforeEach
    void reset() {
        REFERENCED.clear();
    }

    @Test
    @DisplayName("RF-06: edit checks the version and keeps one level of subcategories")
    void edit() {
        Category parent = save.create("Casa", null, "#000000", "house");
        Category child = save.create("Luz", parent.id(), "#000000", "bolt");
        Category edited = save.update(child.id(), "Energia", parent.id(), "#111111", "zap", child.version());
        assertThat(edited.name()).isEqualTo("Energia");
        assertThatThrownBy(() -> save.update(child.id(), "X", null, "#111111", "zap", 99L))
                .isInstanceOf(StaleVersionException.class);
        assertThatThrownBy(() -> save.update(parent.id(), "Casa", child.id(), "#000000", "house", null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> save.create("Y", 9999L, "#000000", "x")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> save.create("Y", queries.unclassifiedId(), "#000000", "x"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("RF-07: categories with entries cannot be deleted")
    void referencedCannotBeDeleted() {
        Category used = save.create("Lazer", null, "#000000", "party-popper");
        REFERENCED.add(used.id());
        assertThat(queries.isDeletable(used.id())).isFalse();
        assertThatThrownBy(() -> save.delete(used.id())).isInstanceOf(BusinessRuleException.class);
        REFERENCED.clear();
        save.delete(used.id());
        assertThat(queries.find(used.id())).isEmpty();
        assertThatThrownBy(() -> queries.get(used.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("RF-07: \"not classified\" is created once and listed with the others")
    void unclassifiedOnce() {
        long first = queries.unclassifiedId();
        assertThat(queries.unclassifiedId()).isEqualTo(first);
        assertThat(queries.all()).anySatisfy(view -> assertThat(view.system()).isTrue());
    }
}
