package com.planejamentomensal.member.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the member representation and its state-dependent links (API-09, API-11).
 *
 * <p>{@code delete} only when nothing references the member; otherwise {@code inactivate} (RF-01).</p>
 *
 * @since 1.0
 */
@Component
public class MemberModelAssembler implements RepresentationModelAssembler<Member, MemberModel> {

    private final MemberQueries queries;

    /**
     * Creates the assembler.
     *
     * @param queries member queries
     */
    public MemberModelAssembler(MemberQueries queries) {
        this.queries = queries;
    }

    /**
     * Converts a member.
     *
     * @param member member
     * @return representation with links
     */
    @Override
    public MemberModel toModel(Member member) {
        MemberModel model = new MemberModel(member);
        Link self = linkTo(methodOn(MemberController.class).member(member.id())).withSelfRel();
        model.add(self);
        model.add(linkTo(methodOn(MemberController.class).members(null, null)).withRel("members"));
        model.add(self.withRel(member.active() ? Relations.INACTIVATE : Relations.ACTIVATE));
        if (queries.isDeletable(member.id())) {
            model.add(self.withRel(Relations.DELETE));
        }
        return model;
    }
}
