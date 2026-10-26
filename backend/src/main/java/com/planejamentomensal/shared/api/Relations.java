package com.planejamentomensal.shared.api;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.hateoas.LinkRelation;

/**
 * Custom link relations of the API, in English kebab-case, described in the HAL profile (API-10).
 *
 * <p>The interface shows an action only when its relation is present in a response (API-09).</p>
 *
 * @since 1.0
 */
public enum Relations implements LinkRelation {

    /** Rename the description of a bill entry (create or replace its alias). */
    RENAME("rename", "Rename the displayed description of a bill entry (RF-46 to RF-48)."),
    /** Remove the alias and show the card's original description again. */
    RESTORE_ORIGINAL("restore-original", "Remove the alias and show the original description (RF-50)."),
    /** Confirm a projected installment or an import preview. */
    CONFIRM("confirm", "Confirm a projected installment or an import preview (RN-06, RF-72)."),
    /** Cancel an import preview. */
    CANCEL("cancel", "Discard an import preview without writing anything (RF-68)."),
    /** Undo a confirmed import batch. */
    UNDO("undo", "Undo a whole confirmed import batch (RF-72)."),
    /** Import a bill file into an open card bill. */
    IMPORT("import", "Import a bill file into this card bill (RF-60)."),
    /** Mark a closed card bill as paid. */
    MARK_PAID("mark-paid", "Mark a closed card bill as paid (RF-36)."),
    /** Installments of a purchase. */
    INSTALLMENTS("installments", "Bill entries (installments) of a purchase."),
    /** Previous billing period. */
    PREVIOUS("previous", "Same resource in the previous billing period."),
    /** Next billing period. */
    NEXT("next", "Same resource in the next billing period."),
    /** Summary split by member. */
    BY_MEMBER("by-member", "Monthly summary by member (RF-13)."),
    /** Summary split by category. */
    BY_CATEGORY("by-category", "Monthly summary by category (RF-12)."),
    /** Export a resource. */
    EXPORT("export", "Export the resource as a file."),
    /** Export the monthly spreadsheet. */
    EXPORT_XLSX("export-xlsx", "Monthly spreadsheet of the billing period (XLS-17)."),
    /** Statement as PDF. */
    PDF("pdf", "Member statement as PDF (RF-84)."),
    /** Statement as PNG. */
    PNG("png", "Member statement as PNG (RF-84)."),
    /** Statement as XLSX. */
    XLSX("xlsx", "Member statement as XLSX (RF-84)."),
    /** Send a statement. */
    SEND("send", "Send a member statement (RF-91)."),
    /** Send a statement by e-mail. */
    SEND_EMAIL("send-email", "Send the member statement by e-mail (RF-86)."),
    /** Open WhatsApp with the statement summary. */
    SEND_WHATSAPP("send-whatsapp", "Open WhatsApp with the statement; only when the member has a phone (RF-87)."),
    /** Preview a template or statement. */
    PREVIEW("preview", "Preview a template or a statement (RF-85, TPL-03)."),
    /** Versions of a template. */
    VERSIONS("versions", "Versions of a template (TPL-06)."),
    /** Make a template the default one. */
    MAKE_DEFAULT("make-default", "Make this template the default (TPL-04)."),
    /** Validate one pending review item. */
    VALIDATE("validate", "Validate one pending item of the review queue (RF-76)."),
    /** Validate the selected pending items. */
    VALIDATE_SELECTED("validate-selected", "Validate the selected pending items (RF-77).");

    private final String value;
    private final String description;

    Relations(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Returns the relation name used in {@code _links}.
     *
     * @return kebab-case relation name
     */
    @Override
    public String value() {
        return value;
    }

    /**
     * Returns what following the relation does.
     *
     * @return English description shown in the profile
     */
    public String description() {
        return description;
    }

    /**
     * Finds a relation by its name.
     *
     * @param value kebab-case relation name
     * @return the relation, or empty if it is not a custom relation of this API
     */
    public static Optional<Relations> fromValue(String value) {
        return Arrays.stream(values()).filter(relation -> relation.value.equals(value)).findFirst();
    }
}
