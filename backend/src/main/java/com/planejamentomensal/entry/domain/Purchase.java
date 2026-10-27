package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Auditable;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A purchase (the fact) with its installments as {@link BillEntry} rows (SPEC section 10).
 *
 * <p>Category, member split, note and review state live here, so whatever is set on one installment holds
 * for every installment of the purchase (RN-08). The original description is never changed (RF-46).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "purchase")
public class Purchase implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private long cardId;

    @Column(name = "card_last_digits")
    private String cardLastDigits;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "original_description", nullable = false)
    private String originalDescription;

    @Column(name = "normalized_description", nullable = false)
    private String normalizedDescription;

    @Column(name = "installment_amount", nullable = false)
    private Money installmentAmount;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(name = "first_known_installment", nullable = false)
    private int firstKnownInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryKind kind;

    @Column(name = "bank_category")
    private String bankCategory;

    @Column(name = "purchase_key", nullable = false)
    private String purchaseKey;

    @Column(name = "category_id")
    private Long categoryId;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;

    @Column(name = "refund_of_purchase_id")
    private Long refundOfPurchaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refunded_amount", nullable = false)
    private Money refundedAmount = Money.ZERO;

    @Column(name = "unknown_final", nullable = false)
    private boolean unknownFinal;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_id", nullable = false)
    private List<PurchaseSplit> splits = new ArrayList<>();

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Purchase() {
    }

    /**
     * Creates a purchase.
     *
     * @param data purchase data
     */
    public Purchase(PurchaseData data) {
        this.cardId = data.cardId();
        this.cardLastDigits = data.cardLastDigits();
        this.purchaseDate = Objects.requireNonNull(data.purchaseDate(), "purchaseDate");
        this.originalDescription = data.originalDescription();
        this.normalizedDescription = DescriptionNormalizer.normalize(data.originalDescription());
        this.installmentAmount = data.installmentAmount();
        this.installmentCount = Math.max(1, data.installmentCount());
        this.firstKnownInstallment = Math.max(1, data.firstKnownInstallment());
        this.type = data.type();
        this.kind = data.kind();
        this.bankCategory = data.bankCategory();
        this.purchaseKey = data.purchaseKey();
        this.categoryId = data.categoryId();
        this.note = data.note();
        this.reviewStatus = data.reviewStatus();
        this.unknownFinal = data.unknownFinal();
        this.splits = new ArrayList<>(data.splits());
    }

    /**
     * Total amount of the purchase: installment amount times the installment count (RN-10).
     *
     * @return total
     */
    public Money totalAmount() {
        return installmentAmount.times(installmentCount);
    }

    /**
     * Sets category, split and note, as on the review queue or inline editing (RF-43, RF-76).
     *
     * @param newCategoryId category, or {@code null} for "not classified"
     * @param newSplits     member shares; must not be empty
     * @param newNote       note, or {@code null}
     * @param byUser        {@code true} when the user edited it, so reimports never overwrite it (RN-14)
     */
    public void classify(Long newCategoryId, List<PurchaseSplit> newSplits, String newNote, boolean byUser) {
        if (newSplits.isEmpty()) {
            throw new IllegalArgumentException("A purchase needs at least one member");
        }
        this.categoryId = newCategoryId;
        this.splits.clear();
        this.splits.addAll(newSplits);
        this.note = newNote;
        this.manuallyEdited = manuallyEdited || byUser;
    }

    /**
     * Marks the purchase as validated (RF-76).
     */
    public void markReviewed() {
        this.reviewStatus = ReviewStatus.REVIEWED;
    }

    /**
     * Changes the type, such as accepting a subscription suggestion (RN-23).
     *
     * @param newType new type
     */
    public void changeType(PurchaseType newType) {
        this.type = newType;
    }

    /**
     * Links this credit as a refund of another purchase (RN-26).
     *
     * @param originalId purchase being refunded
     */
    public void linkAsRefundOf(long originalId) {
        this.kind = EntryKind.REFUND;
        this.refundOfPurchaseId = originalId;
    }

    /**
     * Records a refund received for this purchase (RN-26, RN-27).
     *
     * @param refund refunded amount, positive
     */
    public void registerRefund(Money refund) {
        this.refundedAmount = refundedAmount.plus(refund.abs());
        this.refundStatus = refundedAmount.compareTo(totalAmount()) >= 0 ? RefundStatus.FULL : RefundStatus.PARTIAL;
    }

    /**
     * Undoes a refund registration (RF-72 undo).
     *
     * @param refund amount registered before
     */
    public void unregisterRefund(Money refund) {
        this.refundedAmount = refundedAmount.minus(refund.abs());
        if (refundedAmount.isZero() || refundedAmount.isNegative()) {
            this.refundedAmount = Money.ZERO;
            this.refundStatus = RefundStatus.NONE;
        } else {
            this.refundStatus = RefundStatus.PARTIAL;
        }
    }

    /**
     * Returns what is left after refunds (RN-27).
     *
     * @return remaining balance
     */
    public Money remainingAfterRefunds() {
        return totalAmount().minus(refundedAmount);
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the card whose bill holds the purchase.
     *
     * @return card id
     */
    public long cardId() {
        return cardId;
    }

    /**
     * Returns the card final, when known.
     *
     * @return four digits, or {@code null}
     */
    public String cardLastDigits() {
        return cardLastDigits;
    }

    /**
     * Returns the purchase date (same for every installment).
     *
     * @return date
     */
    public LocalDate purchaseDate() {
        return purchaseDate;
    }

    /**
     * Returns the description as on the card; never changed (RF-46).
     *
     * @return original description
     */
    public String originalDescription() {
        return originalDescription;
    }

    /**
     * Returns the normalized description (RN-15).
     *
     * @return normalized description
     */
    public String normalizedDescription() {
        return normalizedDescription;
    }

    /**
     * Returns the amount of each installment.
     *
     * @return installment amount
     */
    public Money installmentAmount() {
        return installmentAmount;
    }

    /**
     * Returns the number of installments (1 for single purchases and subscriptions).
     *
     * @return count
     */
    public int installmentCount() {
        return installmentCount;
    }

    /**
     * Returns the first installment seen by the system; earlier ones are "before the system" (RN-05).
     *
     * @return installment number
     */
    public int firstKnownInstallment() {
        return firstKnownInstallment;
    }

    /**
     * Returns the type.
     *
     * @return single, installment or subscription
     */
    public PurchaseType type() {
        return type;
    }

    /**
     * Returns the kind.
     *
     * @return purchase, credit, refund, IOF or fee
     */
    public EntryKind kind() {
        return kind;
    }

    /**
     * Returns the Itaú category suggestion.
     *
     * @return bank category, or {@code null}
     */
    public String bankCategory() {
        return bankCategory;
    }

    /**
     * Returns the purchase key (RN-11).
     *
     * @return key
     */
    public String purchaseKey() {
        return purchaseKey;
    }

    /**
     * Returns the category.
     *
     * @return category id, or {@code null} for "not classified" (RF-07)
     */
    public Long categoryId() {
        return categoryId;
    }

    /**
     * Returns the note.
     *
     * @return note, or {@code null}
     */
    public String note() {
        return note;
    }

    /**
     * Returns the review state.
     *
     * @return pending or reviewed
     */
    public ReviewStatus reviewStatus() {
        return reviewStatus;
    }

    /**
     * Tells whether the user edited classification data (RN-14).
     *
     * @return {@code true} if edited by hand
     */
    public boolean manuallyEdited() {
        return manuallyEdited;
    }

    /**
     * Returns the purchase this one refunds.
     *
     * @return purchase id, or {@code null}
     */
    public Long refundOfPurchaseId() {
        return refundOfPurchaseId;
    }

    /**
     * Returns how much was refunded.
     *
     * @return refund status
     */
    public RefundStatus refundStatus() {
        return refundStatus;
    }

    /**
     * Returns the refunded amount.
     *
     * @return amount
     */
    public Money refundedAmount() {
        return refundedAmount;
    }

    /**
     * Tells whether the card final was not registered (RF-66).
     *
     * @return {@code true} if the final is unknown
     */
    public boolean unknownFinal() {
        return unknownFinal;
    }

    /**
     * Returns the member shares.
     *
     * @return splits
     */
    public List<PurchaseSplit> splits() {
        return List.copyOf(splits);
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }

    /**
     * Name in the audit log (RNF-05).
     *
     * @return {@code purchase}
     */
    @Override
    public String auditType() {
        return "purchase";
    }
}
