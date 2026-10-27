package com.planejamentomensal.recurrence.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the occurrence representation and its state-dependent links (API-08, API-09).
 *
 * @since 1.0
 */
@Component
public class OccurrenceModelAssembler implements RepresentationModelAssembler<Occurrence, OccurrenceModel> {

    /**
     * Creates the assembler.
     */
    public OccurrenceModelAssembler() {
        // stateless
    }

    /**
     * Converts an occurrence.
     *
     * @param occurrence occurrence
     * @return representation with links
     */
    @Override
    public OccurrenceModel toModel(Occurrence occurrence) {
        OccurrenceModel model = new OccurrenceModel(occurrence);
        model.add(linkTo(methodOn(OccurrenceController.class).occurrence(occurrence.id())).withSelfRel());
        model.add(linkTo(methodOn(RecurrenceRuleController.class).recurrenceRule(occurrence.rule().id()))
                .withRel(Relations.RULE));
        model.add(linkTo(methodOn(OccurrenceController.class).overrideOccurrenceAmount(occurrence.id(), null, null))
                .withRel(Relations.OVERRIDE_AMOUNT));
        Link status = linkTo(methodOn(OccurrenceController.class).changeOccurrenceStatus(occurrence.id(), null)).withSelfRel();
        if (occurrence.status() == OccurrenceStatus.DONE) {
            model.add(status.withRel(Relations.MARK_EXPECTED));
        } else {
            model.add(status.withRel(occurrence.rule().kind() == RuleKind.INCOME ? Relations.MARK_RECEIVED : Relations.MARK_PAID));
        }
        if (occurrence.linkedEntryId() != null) {
            model.add(Link.of("/api/bill-entries/" + occurrence.linkedEntryId(), Relations.LINKED_ENTRY));
        } else if (occurrence.linkCandidates() != null && !occurrence.linkCandidates().isBlank()) {
            model.add(linkTo(methodOn(OccurrenceController.class).chooseOccurrenceLink(occurrence.id(), null))
                    .withRel(Relations.CHOOSE_LINK));
        }
        return model;
    }
}
