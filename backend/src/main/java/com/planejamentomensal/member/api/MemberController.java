package com.planejamentomensal.member.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.member.application.DeleteMemberUseCase;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberRepository;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.shared.api.ActivePatch;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Members (contas / cotas): people or cost centres that own incomes, expenses and entries (RF-01, RF-20,
 * RF-80).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository members;
    private final MemberQueries queries;
    private final SaveMemberUseCase save;
    private final DeleteMemberUseCase delete;
    private final MemberModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param members   member storage
     * @param queries   member queries
     * @param save      create and edit use case
     * @param delete    delete use case
     * @param assembler HAL assembler
     */
    public MemberController(MemberRepository members, MemberQueries queries, SaveMemberUseCase save,
            DeleteMemberUseCase delete, MemberModelAssembler assembler) {
        this.members = members;
        this.queries = queries;
        this.save = save;
        this.delete = delete;
        this.assembler = assembler;
    }

    /**
     * Lists members, sorted by code by default.
     *
     * @param pageable  page, size and sort
     * @param resources paging assembler
     * @return one page of members with paging links
     */
    @GetMapping
    public PagedModel<MemberModel> members(@PageableDefault(size = 50, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable, PagedResourcesAssembler<Member> resources) {
        return resources.toModel(members.findAll(pageable), assembler);
    }

    /**
     * Returns one member with its version as {@code ETag}.
     *
     * @param id member id
     * @return the member
     */
    @GetMapping("/{id}")
    public ResponseEntity<MemberModel> member(@PathVariable long id) {
        Member member = queries.get(id);
        return ETags.ok(assembler.toModel(member), member.version());
    }

    /**
     * Creates a member.
     *
     * @param request member data
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<MemberModel> create(@Valid @RequestBody MemberRequest request) {
        Member member = save.create(request.toDetails());
        MemberModel model = assembler.toModel(member);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(member.version())).body(model);
    }

    /**
     * Replaces the data of a member.
     *
     * @param id      member id
     * @param request new data
     * @param ifMatch version being edited
     * @return the updated member
     */
    @PutMapping("/{id}")
    public ResponseEntity<MemberModel> replace(@PathVariable long id, @Valid @RequestBody MemberRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Member member = save.update(id, request.toDetails(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(member), member.version());
    }

    /**
     * Activates or inactivates a member.
     *
     * @param id      member id
     * @param patch   new active flag
     * @param ifMatch version being edited
     * @return the updated member
     */
    @PatchMapping("/{id}")
    public ResponseEntity<MemberModel> patch(@PathVariable long id, @Valid @RequestBody ActivePatch patch,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Member member = save.setActive(id, patch.active(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(member), member.version());
    }

    /**
     * Deletes a member that nothing references.
     *
     * @param id member id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable long id) {
        delete.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds {@code members} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor membersRootLink() {
        return () -> Link.of(linkTo(methodOn(MemberController.class).members(null, null)).toUri().getPath(), "members");
    }
}
