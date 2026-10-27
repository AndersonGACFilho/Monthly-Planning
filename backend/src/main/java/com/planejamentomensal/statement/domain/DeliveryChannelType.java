package com.planejamentomensal.statement.domain;

/**
 * Ways to deliver a statement (RF-86 to RF-89).
 *
 * @since 1.0
 */
public enum DeliveryChannelType {
    /** E-mail through the configured SMTP server, PDF attached (RF-86). */
    EMAIL,
    /** WhatsApp link with the summary; the PNG is saved and copied by the interface (RF-87). */
    WHATSAPP,
    /** PNG copied to the clipboard by the interface (RF-88). */
    CLIPBOARD,
    /** PDF saved in {@code data/exports/<billing-period>/} (RF-89). */
    FOLDER
}
