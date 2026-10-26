package com.planejamentomensal.shared.domain;

/**
 * Optimistic concurrency check shared by every use case that edits a resource (API-04).
 *
 * @since 1.0
 */
public final class Versions {

    private Versions() {
    }

    /**
     * Checks the version the client edited against the stored one.
     *
     * @param expected version the client edited, or {@code null} when it did not say (nothing is checked)
     * @param current  stored version
     * @throws StaleVersionException if they differ
     */
    public static void check(Long expected, long current) {
        if (expected != null && expected != current) {
            throw new StaleVersionException(expected, current);
        }
    }
}
