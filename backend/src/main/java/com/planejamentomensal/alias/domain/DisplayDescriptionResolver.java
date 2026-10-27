package com.planejamentomensal.alias.domain;

/**
 * Chooses the description shown for an entry: its override, then the alias of its normalized
 * description, then the original description (RF-46 to RF-50, SPEC section 10).
 *
 * @since 1.0
 */
public final class DisplayDescriptionResolver {

    private DisplayDescriptionResolver() {
    }

    /**
     * Resolves the displayed description.
     *
     * @param override name of the entry's override, or {@code null}
     * @param alias    alias name of the description, or {@code null}
     * @param original original description
     * @return the description to show
     */
    public static String resolve(String override, String alias, String original) {
        if (override != null && !override.isBlank()) {
            return override;
        }
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        return original;
    }
}
