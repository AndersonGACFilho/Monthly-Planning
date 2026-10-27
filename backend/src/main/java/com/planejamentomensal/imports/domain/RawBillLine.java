package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One line read from a bill file, before any business rule (SPEC section 13: {@code BillParser} returns
 * these).
 *
 * @param sequence          position in the file, starting at 1
 * @param kind              purchase, credit, IOF or fee
 * @param purchaseDate      date of the purchase (for installments, the original purchase date)
 * @param description       description as on the bill, without the installment suffix (RF-65)
 * @param installmentNumber installment number, or {@code null} for a single purchase
 * @param installmentCount  installment count, or {@code null}
 * @param amount            amount of this line in reais; negative for credits
 * @param holderName        cardholder as on the bill (PDF section title, XLSX "Nome")
 * @param cardLastDigits    card final when the file has it (XLSX), otherwise {@code null}
 * @param bankCategory      Itaú category suggestion (PDF), or {@code null}
 * @param note              extra information, such as dollar amount or PIX interest (RF-64d, RF-64e)
 * @param relatedDescription for IOF lines, the description of the purchase it belongs to (RF-63e)
 * @since 1.0
 */
public record RawBillLine(int sequence, RawLineKind kind, LocalDate purchaseDate, String description,
        Integer installmentNumber, Integer installmentCount, Money amount, String holderName, String cardLastDigits,
        String bankCategory, String note, String relatedDescription) {

    /**
     * Validates the line.
     *
     * @throws NullPointerException if a required field is missing
     */
    public RawBillLine {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(purchaseDate, "purchaseDate");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }

    /**
     * Returns the description normalized by RN-15.
     *
     * @return normalized description
     */
    public String normalizedDescription() {
        return DescriptionNormalizer.normalize(description);
    }

    /**
     * Tells whether the line is an installment {@code n/N}.
     *
     * @return {@code true} if it has an installment
     */
    public boolean isInstallment() {
        return installmentNumber != null;
    }

    /**
     * Returns the installment as shown on screen.
     *
     * @return such as {@code 03/10}, or {@code -} for a single purchase
     */
    public String installmentLabel() {
        return isInstallment() ? "%02d/%02d".formatted(installmentNumber, installmentCount) : "-";
    }
}
