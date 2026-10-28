package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.entry.application.ManualEntryUseCase;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Card entries: list with filters, inline editing, rename, restore, confirmation and manual entries that can
 * be replaced or deleted (RF-40 to RF-51).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/bill-entries")
public class BillEntryController {

    private final EntryQueries queries;
    private final EntryCommands commands;
    private final ManualEntryUseCase manual;
    private final BillEntryModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param queries   entry queries
     * @param commands  entry commands
     * @param manual    manual purchases
     * @param assembler HAL assembler
     */
    public BillEntryController(EntryQueries queries, EntryCommands commands, ManualEntryUseCase manual,
            BillEntryModelAssembler assembler) {
        this.queries = queries;
        this.commands = commands;
        this.manual = manual;
        this.assembler = assembler;
    }

    /**
     * Lists entries of a billing period or card, filtered and sorted (RF-40, RF-41, RF-51, API-06).
     *
     * @param billingPeriod period {@code yyyy-MM}
     * @param card          primary card
     * @param member        member with a share
     * @param category      category
     * @param type          {@code -}, {@code n/N} or {@code *}
     * @param q             text in the original description or alias
     * @param status        {@code PROJECTED}, {@code CONFIRMED} or {@code UNCONFIRMED}
     * @param pageable      page, size and sort (entryDate, purchaseDate, displayDescription, amount, installment,
     *                      status)
     * @param resources     paging assembler
     * @return one page of entries
     */
    @GetMapping
    public PagedModel<BillEntryModel> billEntries(@RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) Long card, @RequestParam(required = false) Long member,
            @RequestParam(required = false) Long category, @RequestParam(required = false) String type,
            @RequestParam(required = false) String q, @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 500, sort = "entryDate") Pageable pageable,
            @Parameter(hidden = true) PagedResourcesAssembler<EntryRow> resources) {
        List<EntryRow> rows = queries.list(new EntryFilter(billingPeriod == null ? null : BillingPeriod.parse(billingPeriod),
                card, member, category, type, q, status)).stream().sorted(comparator(pageable.getSort())).toList();
        int from = (int) Math.min(pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        return resources.toModel(new PageImpl<>(rows.subList(from, to), pageable, rows.size()), assembler);
    }

    /**
     * Returns one entry.
     *
     * @param id entry id
     * @return the entry with the purchase version as {@code ETag}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BillEntryModel> billEntry(@PathVariable long id) {
        EntryRow row = queries.get(id);
        return ETags.ok(assembler.toModel(row), row.version());
    }

    /**
     * Changes category, member split or note; applies to every installment of the purchase (RF-43, RN-08).
     *
     * @param id      entry id
     * @param request changes
     * @param ifMatch purchase version being edited
     * @return the updated entry
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BillEntryModel> editEntry(@PathVariable long id, @Valid @RequestBody EntryEditRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        List<ReviewItem.Share> splits = request.splits() == null ? null : request.splits().stream()
                .map(s -> new ReviewItem.Share(s.memberId(), s.percentage(), s.amount() == null ? null : Money.of(s.amount())))
                .toList();
        EntryRow row = commands.classify(id, request.categoryId(), request.clearCategory(), splits, request.note(),
                ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(row), row.version());
    }

    /**
     * Renames the description, of every entry with it or of this one only (RF-46 to RF-49).
     *
     * @param id      entry id
     * @param request new name, scope and default category
     * @return the updated entry
     */
    @PutMapping("/{id}/alias")
    public BillEntryModel renameEntry(@PathVariable long id, @Valid @RequestBody RenameRequest request) {
        EntryCommands.RenameScope scope = request.scope() == null ? EntryCommands.RenameScope.ALL : request.scope();
        return assembler.toModel(commands.rename(id, request.displayName(), scope, request.defaultCategoryId()));
    }

    /**
     * Shows the card's description again (RF-50).
     *
     * @param id entry id
     * @return the updated entry
     */
    @DeleteMapping("/{id}/alias")
    public BillEntryModel restoreOriginal(@PathVariable long id) {
        return assembler.toModel(commands.restoreOriginal(id));
    }

    /**
     * Confirms a projected installment by hand.
     *
     * @param id entry id
     * @return the confirmed entry
     */
    @PostMapping("/{id}/confirmation")
    public BillEntryModel confirmEntry(@PathVariable long id) {
        return assembler.toModel(commands.confirm(id));
    }

    /**
     * Records a purchase that was not imported, already validated, on the chosen bill or the one the card's cycle
     * gives; installment purchases project the next installments (RF-44, RF-45, RF-79c).
     *
     * @param request purchase
     * @return 201 with the first entry
     */
    @PostMapping
    public ResponseEntity<BillEntryModel> createEntry(@Valid @RequestBody ManualEntryRequest request) {
        BillEntryModel model = assembler.toModel(manual.create(request.toPurchase()));
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    /**
     * Replaces the purchase of a manual entry and all its installments, while no imported bill has seen it.
     *
     * @param id      any entry of the purchase
     * @param request new purchase data
     * @param ifMatch purchase version being edited
     * @return the first entry of the replaced purchase
     */
    @PutMapping("/{id}/purchase")
    public ResponseEntity<BillEntryModel> replaceEntryPurchase(@PathVariable long id,
            @Valid @RequestBody ManualEntryRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        EntryRow row = manual.replace(id, request.toPurchase(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(row), row.version());
    }

    /**
     * Deletes the purchase of a manual entry with all its installments, while no imported bill has seen it.
     *
     * @param id      any entry of the purchase
     * @param ifMatch purchase version the client saw
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeEntry(@PathVariable long id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        manual.delete(id, ETags.expectedVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    private static Comparator<EntryRow> comparator(Sort sort) {
        Comparator<EntryRow> result = null;
        for (Sort.Order order : sort) {
            Comparator<EntryRow> next = switch (order.getProperty()) {
                case "purchaseDate" -> Comparator.comparing(EntryRow::purchaseDate);
                case "displayDescription" -> Comparator.comparing(EntryRow::displayDescription, String.CASE_INSENSITIVE_ORDER);
                case "amount" -> Comparator.comparing(EntryRow::amount);
                case "installment" -> Comparator.comparing(EntryRow::installment);
                case "status" -> Comparator.comparing(EntryRow::status);
                default -> Comparator.comparing(EntryRow::entryDate);
            };
            next = order.isDescending() ? next.reversed() : next;
            result = result == null ? next : result.thenComparing(next);
        }
        return result == null ? Comparator.comparing(EntryRow::entryDate) : result.thenComparing(EntryRow::entryId);
    }

    /**
     * Adds {@code bill-entries} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor billEntriesRootLink() {
        return () -> Link.of("/api/bill-entries", "bill-entries");
    }
}
