package com.planejamentomensal.category.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Language of the initial categories (I18N-07).
 *
 * @param language {@code pt-BR} or {@code en-US}
 * @since 1.0
 */
public record InitialSetRequest(@NotBlank @Pattern(regexp = "pt-BR|en-US") String language) {
}
