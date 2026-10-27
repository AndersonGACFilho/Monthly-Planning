package com.planejamentomensal.expense.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.expense.application.ExpenseFilter;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.expense.domain.VariableExpense;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Variable expenses entered by hand (RF-34, RF-35).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/variable-expenses")
public class VariableExpenseController {

    private final VariableExpenseCommands commands;

    /**
     * Creates the controller.
     *
     * @param commands expense commands
     */
    public VariableExpenseController(VariableExpenseCommands commands) {
        this.commands = commands;
    }

    /**
     * Lists the expenses of a period, newest first, with the filter total (RF-35).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param member        member, optional
     * @param category      category, optional
     * @return expenses and total
     */
    @GetMapping
    public VariableExpenseListModel variableExpenses(@RequestParam String billingPeriod,
            @RequestParam(required = false) Long member, @RequestParam(required = false) Long category) {
        List<VariableExpense> list = commands.list(new ExpenseFilter(BillingPeriod.parse(billingPeriod), member, category));
        VariableExpenseListModel model = new VariableExpenseListModel(list.stream().map(this::toModel).toList(),
                VariableExpenseCommands.total(list).amount());
        model.add(linkTo(methodOn(VariableExpenseController.class).variableExpenses(billingPeriod, member, category))
                .withSelfRel().expand());
        return model;
    }

    /**
     * Returns one expense with its version as {@code ETag}.
     *
     * @param id expense id
     * @return the expense
     */
    @GetMapping("/{id}")
    public ResponseEntity<VariableExpenseModel> variableExpense(@PathVariable long id) {
        VariableExpense expense = commands.get(id);
        return ETags.ok(toModel(expense), expense.version());
    }

    /**
     * Creates an expense (RF-34).
     *
     * @param request expense data
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<VariableExpenseModel> createVariableExpense(@Valid @RequestBody VariableExpenseRequest request) {
        VariableExpense expense = commands.create(request.toDetails());
        VariableExpenseModel model = toModel(expense);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(expense.version())).body(model);
    }

    /**
     * Replaces the data of an expense.
     *
     * @param id      expense id
     * @param request new data
     * @param ifMatch version being edited
     * @return the updated expense
     */
    @PutMapping("/{id}")
    public ResponseEntity<VariableExpenseModel> replaceVariableExpense(@PathVariable long id,
            @Valid @RequestBody VariableExpenseRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        VariableExpense expense = commands.update(id, request.toDetails(), ETags.expectedVersion(ifMatch));
        return ETags.ok(toModel(expense), expense.version());
    }

    /**
     * Deletes an expense.
     *
     * @param id expense id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeVariableExpense(@PathVariable long id) {
        commands.delete(id);
        return ResponseEntity.noContent().build();
    }

    private VariableExpenseModel toModel(VariableExpense expense) {
        VariableExpenseModel model = new VariableExpenseModel(expense);
        Link self = linkTo(methodOn(VariableExpenseController.class).variableExpense(expense.id())).withSelfRel();
        model.add(self);
        model.add(Link.of("/api/members/" + expense.memberId(), "member"));
        if (expense.categoryId() != null) {
            model.add(Link.of("/api/categories/" + expense.categoryId(), "category"));
        }
        model.add(self.withRel(Relations.DELETE));
        return model;
    }

    /**
     * Adds {@code variable-expenses} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor variableExpensesRootLink() {
        return () -> Link.of("/api/variable-expenses{?billingPeriod,member,category}", "variable-expenses");
    }
}
