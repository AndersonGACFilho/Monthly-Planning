package com.planejamentomensal.member.application;

import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to members for other modules and for the member API.
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class MemberQueries {

    private final MemberRepository members;
    private final List<MemberReferences> references;

    /**
     * Creates the queries.
     *
     * @param members    member storage
     * @param references reference checks registered by other modules
     */
    public MemberQueries(MemberRepository members, List<MemberReferences> references) {
        this.members = members;
        this.references = references;
    }

    /**
     * Loads a member.
     *
     * @param id id
     * @return the member
     * @throws NotFoundException if it does not exist
     */
    public Member get(long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("member", id));
    }

    /**
     * Returns the view of a member.
     *
     * @param id id
     * @return the view, or empty
     */
    public Optional<MemberView> find(long id) {
        return members.findById(id).map(MemberView::of);
    }

    /**
     * Lists every member, sorted by code.
     *
     * @return views of all members
     */
    public List<MemberView> all() {
        return members.findAll().stream().map(MemberView::of).sorted(Comparator.comparing(MemberView::code)).toList();
    }

    /**
     * Tells whether a member can be deleted (RF-01).
     *
     * @param id member id
     * @return {@code true} if nothing references it
     */
    public boolean isDeletable(long id) {
        return references.stream().noneMatch(check -> check.isReferenced(id));
    }
}
