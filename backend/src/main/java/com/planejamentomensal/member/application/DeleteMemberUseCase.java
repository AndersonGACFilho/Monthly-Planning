package com.planejamentomensal.member.application;

import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a member that nothing references; otherwise the member must be inactivated (RF-01).
 *
 * @since 1.0
 */
@Service
public class DeleteMemberUseCase {

    private final MemberRepository members;
    private final MemberQueries queries;

    /**
     * Creates the use case.
     *
     * @param members member storage
     * @param queries member queries
     */
    public DeleteMemberUseCase(MemberRepository members, MemberQueries queries) {
        this.members = members;
        this.queries = queries;
    }

    /**
     * Deletes the member.
     *
     * @param id member id
     * @throws BusinessRuleException with code {@code member-in-use} if entries or other data point to it
     */
    @Transactional
    public void delete(long id) {
        Member member = queries.get(id);
        if (!queries.isDeletable(id)) {
            throw new BusinessRuleException("member-in-use", member.code());
        }
        members.delete(member);
    }
}
