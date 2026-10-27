package com.planejamentomensal.entry.domain;

/**
 * Life cycle of a bill entry (RN-04 to RN-07).
 *
 * @since 1.0
 */
public enum EntryStatus {
    /** Created ahead for a future bill (RN-04, RN-09). */
    PROJECTED,
    /** Seen on an imported bill or entered by hand. */
    CONFIRMED,
    /** Projected but missing from the imported bill; needs review (RN-07, RN-21). */
    UNCONFIRMED
}
