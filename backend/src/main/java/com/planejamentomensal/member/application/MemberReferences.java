package com.planejamentomensal.member.application;

/**
 * Tells whether something in another module points to a member (RF-01: a member with entries cannot be
 * deleted). Each module that stores member ids registers one implementation (SOL-02).
 *
 * @since 1.0
 */
public interface MemberReferences {

    /**
     * Tells whether the member is used.
     *
     * @param memberId member id
     * @return {@code true} if deleting the member would lose history
     */
    boolean isReferenced(long memberId);
}
