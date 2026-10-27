package com.planejamentomensal.recurrence.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the rule representation and its state-dependent links (API-08, API-09).
 *
 * @since 1.0
 */
@Component
public class RecurrenceRuleModelAssembler implements RepresentationModelAssembler<RecurrenceRule, RecurrenceRuleModel> {

    private final RecurrenceCommands commands;

    /**
     * Creates the assembler.
     *
     * @param commands rule commands
     */
    public RecurrenceRuleModelAssembler(RecurrenceCommands commands) {
        this.commands = commands;
    }

    /**
     * Converts a rule.
     *
     * @param rule rule
     * @return representation with links
     */
    @Override
    public RecurrenceRuleModel toModel(RecurrenceRule rule) {
        RecurrenceRuleModel model = new RecurrenceRuleModel(rule);
        Link self = linkTo(methodOn(RecurrenceRuleController.class).recurrenceRule(rule.id())).withSelfRel();
        model.add(self);
        model.add(Link.of("/api/recurrence-rules?kind=" + rule.kind(), "recurrence-rules"));
        model.add(Link.of("/api/members/" + rule.memberId(), "member"));
        if (rule.categoryId() != null) {
            model.add(Link.of("/api/categories/" + rule.categoryId(), "category"));
        }
        model.add(self.withRel(rule.active() ? Relations.INACTIVATE : Relations.ACTIVATE));
        if (commands.isDeletable(rule.id())) {
            model.add(self.withRel(Relations.DELETE));
        }
        return model;
    }
}
