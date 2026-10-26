package com.planejamentomensal.member.application;

import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.shared.domain.Versions;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or edits a member, keeping codes unique (RF-01).
 *
 * @since 1.0
 */
@Service
public class SaveMemberUseCase {

    private final MemberRepository members;
    private final MemberQueries queries;

    /**
     * Creates the use case.
     *
     * @param members member storage
     * @param queries member queries
     */
    public SaveMemberUseCase(MemberRepository members, MemberQueries queries) {
        this.members = members;
        this.queries = queries;
    }

    /**
     * Creates a member.
     *
     * @param details member data
     * @return the stored member
     * @throws BusinessRuleException with code {@code member-code-taken} if the code is in use
     */
    @Transactional
    public Member create(MemberDetails details) {
        ensureCodeFree(details.code(), null);
        return members.saveAndFlush(new Member(details));
    }

    /**
     * Replaces the data of a member.
     *
     * @param id      member id
     * @param details new data
     * @param expectedVersion version the client edited, or {@code null}
     * @return the updated member
     * @throws BusinessRuleException with code {@code member-code-taken} if the code is in use
     */
    @Transactional
    public Member update(long id, MemberDetails details, Long expectedVersion) {
        Member member = queries.get(id);
        Versions.check(expectedVersion, member.version());
        ensureCodeFree(details.code(), id);
        member.update(details);
        return members.saveAndFlush(member);
    }

    /**
     * Activates or inactivates a member (RF-01).
     *
     * @param id      member id
     * @param active  new state
     * @param expectedVersion version the client edited, or {@code null}
     * @return the updated member
     */
    @Transactional
    public Member setActive(long id, boolean active, Long expectedVersion) {
        Member member = queries.get(id);
        Versions.check(expectedVersion, member.version());
        member.setActive(active);
        return members.saveAndFlush(member);
    }

    private void ensureCodeFree(String code, Long ownId) {
        members.findByCode(code)
                .filter(other -> !other.id().equals(ownId))
                .ifPresent(other -> {
                    throw new BusinessRuleException("member-code-taken", code);
                });
    }
}
