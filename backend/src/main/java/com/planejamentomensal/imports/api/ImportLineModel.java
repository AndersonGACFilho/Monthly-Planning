package com.planejamentomensal.imports.api;

import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.domain.LineDecision;
import com.planejamentomensal.imports.domain.LineStatus;
import com.planejamentomensal.imports.domain.RawLineKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a preview line (RF-68).
 *
 * @since 1.0
 */
@Relation(itemRelation = "line", collectionRelation = "lines")
public class ImportLineModel extends RepresentationModel<ImportLineModel> {

    /** Id. */
    public final long id;
    /** Position in the file. */
    public final int sequence;
    /** Kind. */
    public final RawLineKind kind;
    /** Purchase date. */
    public final LocalDate purchaseDate;
    /** Description as on the file. */
    public final String description;
    /** Installment: {@code n/N} or {@code -}. */
    public final String installment;
    /** Amount. */
    public final BigDecimal amount;
    /** Holder. */
    public final String holderName;
    /** Card final. */
    public final String cardLastDigits;
    /** Final not registered (RF-66). */
    public final boolean unknownFinal;
    /** Itaú category suggestion. */
    public final String bankCategory;
    /** Note. */
    public final String note;
    /** Only used to check projections (RF-64g). */
    public final boolean futureCheck;
    /** Classification (RN-13). */
    public final LineStatus status;
    /** Decision (RF-69). */
    public final LineDecision decision;
    /** Existing entry the line refers to. */
    public final Long matchedEntryId;
    /** Member. */
    public final Long memberId;
    /** Category. */
    public final Long categoryId;
    /** Alias to apply (RF-70). */
    public final String aliasName;
    /** Identical lines get 2, 3… (RN-12). */
    public final int occurrence;

    /**
     * Copies a line.
     *
     * @param line line
     */
    public ImportLineModel(ImportLine line) {
        this.id = line.id();
        this.sequence = line.sequence();
        this.kind = line.kind();
        this.purchaseDate = line.purchaseDate();
        this.description = line.description();
        this.installment = line.installmentLabel();
        this.amount = line.amount().amount();
        this.holderName = line.holderName();
        this.cardLastDigits = line.cardLastDigits();
        this.unknownFinal = line.unknownFinal();
        this.bankCategory = line.bankCategory();
        this.note = line.note();
        this.futureCheck = line.futureCheck();
        this.status = line.status();
        this.decision = line.decision();
        this.matchedEntryId = line.matchedEntryId();
        this.memberId = line.memberId();
        this.categoryId = line.categoryId();
        this.aliasName = line.aliasName();
        this.occurrence = line.occurrence();
    }
}
