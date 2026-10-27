package com.planejamentomensal.entry.application;

import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.entry.domain.PurchaseType;
import com.planejamentomensal.entry.domain.ReviewStatus;
import com.planejamentomensal.entry.domain.SplitPolicy;
import com.planejamentomensal.entry.domain.SubscriptionSuggester;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.Versions;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The review queue: lists pending purchases with suggestions and validates them one by one or in batch
 * (RF-74 to RF-79a, RN-23; {@code ValidateEntriesUseCase} of SPEC section 8).
 *
 * @since 1.0
 */
@Service
@Transactional
public class ReviewQueue {

    private final PurchaseRepository purchases;
    private final BillEntryRepository entries;
    private final MemberQueries members;
    private final CategoryQueries categories;
    private final EntryLedger ledger;

    /**
     * Creates the queue.
     *
     * @param purchases  purchase storage
     * @param entries    entry storage
     * @param members    member queries, for the MAIN rule of RF-76
     * @param categories category queries, for "not classified"
     * @param ledger     ledger, to project accepted subscriptions (RN-09)
     */
    public ReviewQueue(PurchaseRepository purchases, BillEntryRepository entries, MemberQueries members,
            CategoryQueries categories, EntryLedger ledger) {
        this.purchases = purchases;
        this.entries = entries;
        this.members = members;
        this.categories = categories;
        this.ledger = ledger;
    }

    /**
     * Totals of the queue, shown in the sidebar and on the overview (RF-79, RF-79a).
     *
     * @param count number of pending purchases
     * @param total sum of their entries
     */
    public record Summary(int count, Money total) {
    }

    /**
     * Lists pending purchases, optionally filtered (RF-79).
     *
     * @param cardId        card, or {@code null}
     * @param billingPeriod period, or {@code null}
     * @param batchId       import batch, or {@code null}
     * @return pending items, oldest purchase first
     */
    @Transactional(readOnly = true)
    public List<ReviewItem> pending(Long cardId, BillingPeriod billingPeriod, Long batchId) {
        return purchases.findByReviewStatus(ReviewStatus.PENDING_REVIEW).stream()
                .filter(p -> cardId == null || p.cardId() == cardId)
                .map(this::item)
                .flatMap(Optional::stream)
                .filter(i -> billingPeriod == null || i.billingPeriod().equals(billingPeriod))
                .filter(i -> batchId == null || batchId.equals(i.importBatchId()))
                .sorted(Comparator.comparing(ReviewItem::purchaseDate).thenComparing(ReviewItem::purchaseId))
                .toList();
    }

    /**
     * Counts pending purchases and sums them (RF-79a).
     *
     * @param billingPeriod period, or {@code null} for all
     * @return count and total
     */
    @Transactional(readOnly = true)
    public Summary summary(BillingPeriod billingPeriod) {
        List<ReviewItem> items = pending(null, billingPeriod, null);
        return new Summary(items.size(), items.stream().map(ReviewItem::amount).reduce(Money.ZERO, Money::plus));
    }

    /**
     * Returns one item.
     *
     * @param purchaseId purchase
     * @return the item
     */
    @Transactional(readOnly = true)
    public ReviewItem item(long purchaseId) {
        return item(purchase(purchaseId)).orElseThrow(() -> new NotFoundException("review-item", purchaseId));
    }

    /**
     * Applies the same edit to several purchases, optionally validating them (RF-76, RF-77).
     *
     * @param purchaseIds     purchases
     * @param edit            changes; {@code null} fields keep their value
     * @param validate        {@code true} to mark them as reviewed
     * @param expectedVersion version of a single edited item, or {@code null}
     * @return the updated items
     * @throws BusinessRuleException with code {@code review-split-incomplete} or {@code review-category-required}
     */
    public List<ReviewItem> apply(List<Long> purchaseIds, ReviewEdit edit, boolean validate, Long expectedVersion) {
        return purchaseIds.stream().map(id -> {
            Purchase purchase = purchase(id);
            if (purchaseIds.size() == 1) {
                Versions.check(expectedVersion, purchase.version());
            }
            List<PurchaseSplit> splits = edit.splits() == null ? purchase.splits()
                    : edit.splits().stream().map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList();
            Long category = edit.clearCategory() ? null : edit.categoryId() != null ? edit.categoryId() : purchase.categoryId();
            String note = edit.note() != null ? (edit.note().isBlank() ? null : edit.note().strip()) : purchase.note();
            if (validate) {
                check(purchase, splits, category);
            }
            purchase.classify(category, copy(splits), note, true);
            if (validate) {
                purchase.markReviewed();
            }
            purchases.save(purchase);
            if (edit.subscription() && purchase.type() == PurchaseType.SINGLE) {
                ledger.makeSubscription(purchase.id());
            }
            return item(purchase).orElseThrow();
        }).toList();
    }

    private void check(Purchase purchase, List<PurchaseSplit> splits, Long category) {
        if (!SplitPolicy.isComplete(purchase.installmentAmount(), splits)) {
            throw new BusinessRuleException("review-split-incomplete", purchase.originalDescription());
        }
        boolean hasMain = splits.stream().map(s -> members.find(s.memberId())).flatMap(Optional::stream)
                .anyMatch(MemberView::main);
        boolean classified = category != null && category != categories.unclassifiedId();
        if (hasMain && !classified) {
            throw new BusinessRuleException("review-category-required", purchase.originalDescription());
        }
    }

    private Optional<ReviewItem> item(Purchase p) {
        List<BillEntry> installments = entries.findByPurchaseIdOrderByEntryDate(p.id());
        Optional<BillEntry> current = installments.stream()
                .filter(e -> e.status() == EntryStatus.CONFIRMED)
                .max(Comparator.comparing((BillEntry e) -> e.cardBill().billingPeriod()))
                .or(() -> installments.stream().findFirst());
        return current.map(e -> new ReviewItem(p.id(), e.id(), p.cardId(), p.cardLastDigits(), p.unknownFinal(),
                e.cardBill().billingPeriod(), p.purchaseDate(), p.originalDescription(), e.installmentLabel(), e.amount(),
                p.kind().name(), p.bankCategory(), p.categoryId(),
                p.splits().stream().map(s -> new ReviewItem.Share(s.memberId(), s.percentage(), s.amount())).toList(),
                p.note(), p.refundOfPurchaseId(), e.importBatchId(),
                p.type() == PurchaseType.SINGLE && !e.amount().isNegative() && looksLikeSubscription(p, e), p.version()));
    }

    private boolean looksLikeSubscription(Purchase purchase, BillEntry entry) {
        BillingPeriod previous = entry.cardBill().billingPeriod().previous();
        Map<Long, List<BillEntry>> byPurchase = entries.findByCardBillCardId(purchase.cardId()).stream()
                .filter(e -> e.cardBill().billingPeriod().equals(previous))
                .collect(Collectors.groupingBy(e -> e.purchase().id()));
        List<SubscriptionSuggester.PreviousCharge> charges = byPurchase.values().stream()
                .map(list -> list.getFirst())
                .map(e -> new SubscriptionSuggester.PreviousCharge(e.purchase().originalDescription(), e.amount(),
                        e.purchase().type() != PurchaseType.INSTALLMENT))
                .toList();
        return SubscriptionSuggester.looksLikeSubscription(purchase.originalDescription(), entry.amount(), charges);
    }

    private Purchase purchase(long id) {
        return purchases.findById(id).orElseThrow(() -> new NotFoundException("purchase", id));
    }

    private static List<PurchaseSplit> copy(List<PurchaseSplit> splits) {
        return splits.stream().map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList();
    }
}
