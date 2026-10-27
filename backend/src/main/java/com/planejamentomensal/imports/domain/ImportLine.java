package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

/**
 * One line of an import preview: what was read, its classification and the user's decision (RF-68 to
 * RF-70). Kept in the database between preview and confirmation instead of the file (RNF-04, Q16).
 *
 * @since 1.0
 */
@Entity
@Table(name = "import_line")
public class ImportLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private long batchId;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RawLineKind kind;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private String description;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(nullable = false)
    private Money amount;

    @Column(name = "holder_name")
    private String holderName;

    @Column(name = "card_last_digits")
    private String cardLastDigits;

    @Column(name = "bank_category")
    private String bankCategory;

    private String note;

    @Column(name = "related_description")
    private String relatedDescription;

    @Column(name = "future_check", nullable = false)
    private boolean futureCheck;

    @Column(nullable = false)
    private int occurrence;

    @Column(name = "purchase_key", nullable = false)
    private String purchaseKey;

    @Column(name = "installment_key", nullable = false)
    private String installmentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineStatus status;

    @Column(name = "matched_entry_id")
    private Long matchedEntryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineDecision decision;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "alias_name")
    private String aliasName;

    @Column(name = "unknown_final", nullable = false)
    private boolean unknownFinal;

    @Column(name = "created_entry_id")
    private Long createdEntryId;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected ImportLine() {
    }

    /**
     * Creates a preview line.
     *
     * @param batchId        batch
     * @param line           line read from the file
     * @param futureCheck    {@code true} for "next bills" installments, only used for checking (RF-64g)
     * @param occurrence     occurrence of identical lines (RN-12)
     * @param purchaseKey    purchase key (RN-11)
     * @param installmentKey installment key (RN-10)
     * @param classification status and matched entry (RN-13)
     * @param memberId       member resolved from holder or final (RF-66)
     * @param unknownFinal   final not registered (RF-66)
     */
    public ImportLine(long batchId, RawBillLine line, boolean futureCheck, int occurrence, String purchaseKey,
            String installmentKey, Classification classification, Long memberId, boolean unknownFinal) {
        this.batchId = batchId;
        this.sequence = line.sequence();
        this.kind = line.kind();
        this.purchaseDate = line.purchaseDate();
        this.description = line.description();
        this.installmentNumber = line.installmentNumber();
        this.installmentCount = line.installmentCount();
        this.amount = line.amount();
        this.holderName = line.holderName();
        this.cardLastDigits = line.cardLastDigits();
        this.bankCategory = line.bankCategory();
        this.note = line.note();
        this.relatedDescription = line.relatedDescription();
        this.futureCheck = futureCheck;
        this.occurrence = occurrence;
        this.purchaseKey = purchaseKey;
        this.installmentKey = installmentKey;
        this.status = classification.status();
        this.matchedEntryId = classification.matchedEntryId();
        this.decision = futureCheck ? LineDecision.IGNORE : LineDecision.defaultFor(classification.status());
        this.memberId = memberId;
        this.unknownFinal = unknownFinal;
    }

    /**
     * Changes the decision (RF-69).
     *
     * @param newDecision decision
     * @throws IllegalArgumentException if updating is chosen for a line that is not divergent
     */
    public void decide(LineDecision newDecision) {
        if (newDecision == LineDecision.UPDATE_EXISTING && status != LineStatus.DIVERGENT) {
            throw new IllegalArgumentException("Only divergent lines can update an existing entry");
        }
        this.decision = newDecision;
    }

    /**
     * Sets suggestions or user edits of member, category and alias before confirming (RF-70).
     *
     * @param newMemberId   member, or {@code null} to keep
     * @param newCategoryId category, or {@code null} to keep
     * @param newAliasName  alias, or {@code null} to keep
     */
    public void edit(Long newMemberId, Long newCategoryId, String newAliasName) {
        if (newMemberId != null) {
            this.memberId = newMemberId;
        }
        if (newCategoryId != null) {
            this.categoryId = newCategoryId;
        }
        if (newAliasName != null) {
            this.aliasName = newAliasName.isBlank() ? null : newAliasName.strip();
        }
    }

    /**
     * Records the entry created or confirmed for this line.
     *
     * @param entryId entry
     */
    public void recordEntry(Long entryId) {
        this.createdEntryId = entryId;
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
     * Returns the batch.
     *
     * @return batch id
     */
    public long batchId() {
        return batchId;
    }

    /**
     * Returns the position in the file.
     *
     * @return sequence
     */
    public int sequence() {
        return sequence;
    }

    /**
     * Returns the kind.
     *
     * @return kind
     */
    public RawLineKind kind() {
        return kind;
    }

    /**
     * Returns the purchase date.
     *
     * @return date
     */
    public LocalDate purchaseDate() {
        return purchaseDate;
    }

    /**
     * Returns the description as on the file.
     *
     * @return description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the installment number.
     *
     * @return number, or {@code null}
     */
    public Integer installmentNumber() {
        return installmentNumber;
    }

    /**
     * Returns the installment count.
     *
     * @return count, or {@code null}
     */
    public Integer installmentCount() {
        return installmentCount;
    }

    /**
     * Returns the installment as shown.
     *
     * @return {@code n/N} or {@code -}
     */
    public String installmentLabel() {
        return installmentNumber == null ? "-" : "%02d/%02d".formatted(installmentNumber, installmentCount);
    }

    /**
     * Returns the amount.
     *
     * @return amount
     */
    public Money amount() {
        return amount;
    }

    /**
     * Returns the holder name.
     *
     * @return holder, or {@code null}
     */
    public String holderName() {
        return holderName;
    }

    /**
     * Returns the card final.
     *
     * @return final, or {@code null}
     */
    public String cardLastDigits() {
        return cardLastDigits;
    }

    /**
     * Returns the Itaú category suggestion.
     *
     * @return category, or {@code null}
     */
    public String bankCategory() {
        return bankCategory;
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
     * Returns the description of the purchase an IOF line belongs to.
     *
     * @return description, or {@code null}
     */
    public String relatedDescription() {
        return relatedDescription;
    }

    /**
     * Tells whether the line only serves to check projections (RF-64g).
     *
     * @return flag
     */
    public boolean futureCheck() {
        return futureCheck;
    }

    /**
     * Returns the occurrence among identical lines (RN-12).
     *
     * @return occurrence
     */
    public int occurrence() {
        return occurrence;
    }

    /**
     * Returns the purchase key.
     *
     * @return key
     */
    public String purchaseKey() {
        return purchaseKey;
    }

    /**
     * Returns the installment key.
     *
     * @return key
     */
    public String installmentKey() {
        return installmentKey;
    }

    /**
     * Returns the classification.
     *
     * @return status
     */
    public LineStatus status() {
        return status;
    }

    /**
     * Returns the existing entry the line refers to.
     *
     * @return entry id, or {@code null}
     */
    public Long matchedEntryId() {
        return matchedEntryId;
    }

    /**
     * Returns the decision.
     *
     * @return decision
     */
    public LineDecision decision() {
        return decision;
    }

    /**
     * Returns the member.
     *
     * @return member id, or {@code null}
     */
    public Long memberId() {
        return memberId;
    }

    /**
     * Returns the category.
     *
     * @return category id, or {@code null}
     */
    public Long categoryId() {
        return categoryId;
    }

    /**
     * Returns the alias to apply.
     *
     * @return alias, or {@code null}
     */
    public String aliasName() {
        return aliasName;
    }

    /**
     * Tells whether the final is not registered (RF-66).
     *
     * @return flag
     */
    public boolean unknownFinal() {
        return unknownFinal;
    }

    /**
     * Returns the entry created or confirmed on confirmation.
     *
     * @return entry id, or {@code null}
     */
    public Long createdEntryId() {
        return createdEntryId;
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}
