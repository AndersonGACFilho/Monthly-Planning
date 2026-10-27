package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.Money;

/**
 * Total of a bill section as printed and as computed from the lines read (RF-64h).
 *
 * @param section  section name, such as "Lançamentos no cartão"
 * @param declared total printed on the bill
 * @param computed sum of the lines read in that section
 * @since 1.0
 */
public record SectionTotal(String section, Money declared, Money computed) {

    /**
     * Tells whether printed and computed totals are equal.
     *
     * @return {@code true} if they match to the cent
     */
    public boolean matches() {
        return declared.equals(computed);
    }
}
