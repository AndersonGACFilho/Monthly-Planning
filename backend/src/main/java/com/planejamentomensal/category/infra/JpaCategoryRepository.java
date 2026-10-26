package com.planejamentomensal.category.infra;

import com.planejamentomensal.category.application.CategoryRepository;
import com.planejamentomensal.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link CategoryRepository}.
 *
 * @since 1.0
 */
public interface JpaCategoryRepository extends JpaRepository<Category, Long>, CategoryRepository {
}
