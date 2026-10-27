package com.planejamentomensal.recurrence.api;

import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.api.ActivePatch;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parameterized incomes (RF-21, RF-23) and fixed expenses (RF-30); both kinds share this resource.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/recurrence-rules")
public class RecurrenceRuleController {

    private final RecurrenceCommands commands;
    private final RecurrenceRuleModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param commands  rule commands
     * @param assembler HAL assembler
     */
    public RecurrenceRuleController(RecurrenceCommands commands, RecurrenceRuleModelAssembler assembler) {
        this.commands = commands;
        this.assembler = assembler;
    }

    /**
     * Lists the rules of a kind, by description.
     *
     * @param kind {@code INCOME} or {@code FIXED_EXPENSE}
     * @return rules
     */
    @GetMapping
    public CollectionModel<RecurrenceRuleModel> recurrenceRules(@RequestParam RuleKind kind) {
        List<RecurrenceRule> list = commands.rules(kind).stream()
                .sorted(Comparator.comparing(RecurrenceRule::description, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return assembler.toCollectionModel(list)
                .add(Link.of("/api/recurrence-rules?kind=" + kind).withSelfRel());
    }

    /**
     * Returns one rule with its version as {@code ETag}.
     *
     * @param id rule id
     * @return the rule
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurrenceRuleModel> recurrenceRule(@PathVariable long id) {
        RecurrenceRule rule = commands.rule(id);
        return ETags.ok(assembler.toModel(rule), rule.version());
    }

    /**
     * Creates an income or fixed expense rule; a one-off income uses recurrence {@code ONCE} (RF-23).
     *
     * @param kind    {@code INCOME} or {@code FIXED_EXPENSE}
     * @param request rule data
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<RecurrenceRuleModel> createRecurrenceRule(@RequestParam RuleKind kind,
            @Valid @RequestBody RecurrenceRuleRequest request) {
        RecurrenceRule rule = commands.create(kind, request.toDetails());
        RecurrenceRuleModel model = assembler.toModel(rule);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(rule.version())).body(model);
    }

    /**
     * Replaces the data of a rule; periods changed by hand keep their amounts (RF-22).
     *
     * @param id      rule id
     * @param request new data
     * @param ifMatch version being edited
     * @return the updated rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurrenceRuleModel> replaceRecurrenceRule(@PathVariable long id,
            @Valid @RequestBody RecurrenceRuleRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        RecurrenceRule rule = commands.update(id, request.toDetails(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(rule), rule.version());
    }

    /**
     * Activates or inactivates a rule; inactive rules stop generating new periods.
     *
     * @param id    rule id
     * @param patch new active flag
     * @return the updated rule
     */
    @PatchMapping("/{id}")
    public ResponseEntity<RecurrenceRuleModel> patchRecurrenceRule(@PathVariable long id,
            @Valid @RequestBody ActivePatch patch) {
        RecurrenceRule rule = commands.setActive(id, patch.active());
        return ETags.ok(assembler.toModel(rule), rule.version());
    }

    /**
     * Deletes a rule without received or paid periods.
     *
     * @param id rule id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeRecurrenceRule(@PathVariable long id) {
        commands.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds {@code incomes} and {@code fixed-expenses} rule lists to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor recurrenceRulesRootLink() {
        return () -> Link.of("/api/recurrence-rules{?kind}", "recurrence-rules");
    }
}
