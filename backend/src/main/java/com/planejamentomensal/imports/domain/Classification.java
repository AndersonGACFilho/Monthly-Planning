package com.planejamentomensal.imports.domain;

/**
 * Result of classifying one imported line (RN-13).
 *
 * @param status         new, duplicate, matched or divergent
 * @param matchedEntryId existing entry it refers to, or {@code null} for new lines
 * @since 1.0
 */
public record Classification(LineStatus status, Long matchedEntryId) {

    /**
     * A new line.
     *
     * @return classification without an existing entry
     */
    public static Classification newLine() {
        return new Classification(LineStatus.NEW, null);
    }
}
