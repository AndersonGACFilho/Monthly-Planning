package com.planejamentomensal.member.application;

import com.planejamentomensal.member.domain.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Storage port of members (SOL-04).
 *
 * @since 1.0
 */
public interface MemberRepository {

    /**
     * Finds a member.
     *
     * @param id id
     * @return the member, or empty
     */
    Optional<Member> findById(Long id);

    /**
     * Lists members.
     *
     * @param pageable page and sort
     * @return one page
     */
    Page<Member> findAll(Pageable pageable);

    /**
     * Lists every member.
     *
     * @return all members
     */
    List<Member> findAll();

    /**
     * Finds a member by code.
     *
     * @param code code
     * @return the member, or empty
     */
    Optional<Member> findByCode(String code);

    /**
     * Stores a member.
     *
     * @param member member
     * @return the stored member
     */
    Member save(Member member);

    /**
     * Stores a member and writes it at once, so its new version is known (API-04).
     *
     * @param member member
     * @return the stored member
     */
    Member saveAndFlush(Member member);

    /**
     * Removes a member.
     *
     * @param member member
     */
    void delete(Member member);
}
