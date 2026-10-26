package com.planejamentomensal.member.domain;

/**
 * Whether a member is the household itself or someone who reimburses (XLS-04).
 *
 * @since 1.0
 */
public enum MemberKind {
    /** Household member whose expenses are the family's own (such as AB). */
    MAIN,
    /** Third party who pays back the share assigned to them (such as M, T, AP). */
    THIRD_PARTY
}
