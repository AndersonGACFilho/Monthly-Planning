package com.planejamentomensal.summary.domain;

/**
 * Member as the summary sees it.
 *
 * @param id   member
 * @param main {@code true} for a MAIN member, {@code false} for a third party who reimburses (XLS-04)
 * @since 1.0
 */
public record MemberKindInfo(long id, boolean main) {
}
