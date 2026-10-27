package com.planejamentomensal.imports.domain;

/**
 * What a line read from a bill is.
 *
 * @since 1.0
 */
public enum RawLineKind {
    /** Purchase or installment (positive amount). */
    PURCHASE,
    /** Refund or credit (negative amount that is not a payment, RF-63d, RF-67). */
    CREDIT,
    /** IOF of an international purchase or of the whole bill (RF-63e, RF-64d). */
    IOF,
    /** Fee or product charged by the bank, such as the card plan (RF-64c "produtos e serviços"). */
    FEE
}
