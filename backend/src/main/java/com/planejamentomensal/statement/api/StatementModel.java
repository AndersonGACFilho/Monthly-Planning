package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.application.PreparedStatement;
import com.planejamentomensal.statement.domain.MemberStatement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

/**
 * HAL representation of a member statement, for the preview before sending or downloading (RF-82, RF-85).
 *
 * @since 1.0
 */
public class StatementModel extends RepresentationModel<StatementModel> {

    /**
     * One card entry.
     *
     * @param card        card nickname
     * @param date        date
     * @param description description
     * @param installment installment label
     * @param amount      member share
     * @param percent     share percentage, or {@code null}
     * @param category    category, or {@code null}
     * @param pending     pending review
     */
    public record Line(String card, LocalDate date, String description, String installment, BigDecimal amount,
            BigDecimal percent, String category, boolean pending) {
    }

    /** Member. */
    public final long memberId;
    /** Member code. */
    public final String memberCode;
    /** Member name. */
    public final String memberName;
    /** Billing period. */
    public final String billingPeriod;
    /** Language of the statement. */
    public final String language;
    /** Template used. */
    public final long templateId;
    /** Template version used. */
    public final int templateVersion;
    /** {@code true} when the template had no texts in this language (I18N-11). */
    public final boolean languageFallback;
    /** Card entries. */
    public final List<Line> lines;
    /** Card total. */
    public final BigDecimal cardTotal;
    /** Fixed expenses total. */
    public final BigDecimal fixedTotal;
    /** Variable expenses total. */
    public final BigDecimal variableTotal;
    /** Amount still owed from the previous period, or {@code null} (RF-28). */
    public final BigDecimal previousBalance;
    /** Amount carried into the next period, or {@code null} (RF-28). */
    public final BigDecimal carriedOut;
    /** Total due, previous balance included. */
    public final BigDecimal total;
    /** Earliest due date. */
    public final LocalDate dueDate;
    /** PIX key shown. */
    public final String pixKey;
    /** Entries still pending review (RF-79b). */
    public final List<String> pendingItems;
    /** E-mail subject. */
    public final String subject;
    /** WhatsApp message. */
    public final String whatsappText;

    /**
     * Copies a prepared statement.
     *
     * @param p prepared statement
     */
    public StatementModel(PreparedStatement p) {
        MemberStatement s = p.statement();
        this.memberId = s.memberId();
        this.memberCode = s.memberCode();
        this.memberName = s.memberName();
        this.billingPeriod = s.billingPeriod().toString();
        this.language = p.rendered().language();
        this.templateId = p.template().templateId();
        this.templateVersion = p.template().version();
        this.languageFallback = p.rendered().fallback();
        this.lines = s.cards().stream().flatMap(g -> g.lines().stream().map(l -> new Line(g.cardName(), l.date(),
                l.description(), l.installment(), l.amount().amount(), l.percent(), l.category(), l.pending()))).toList();
        this.cardTotal = s.cardTotal().amount();
        this.fixedTotal = s.fixedTotal().amount();
        this.variableTotal = s.variableTotal().amount();
        this.previousBalance = s.previousBalance() == null ? null : s.previousBalance().amount().amount();
        this.carriedOut = s.carriedOut() == null ? null : s.carriedOut().amount().amount();
        this.total = s.total().amount();
        this.dueDate = s.dueDate();
        this.pixKey = s.pixKey();
        this.pendingItems = s.pendingItems();
        this.subject = p.rendered().subject();
        this.whatsappText = p.rendered().whatsappText();
    }
}
