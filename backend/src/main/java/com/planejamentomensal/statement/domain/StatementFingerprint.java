package com.planejamentomensal.statement.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fingerprint of what a statement says, without its issue date: when it changes after a delivery, the
 * delivery is outdated (RF-93).
 *
 * @since 1.0
 */
public final class StatementFingerprint {

    private StatementFingerprint() {
    }

    /**
     * Calculates the fingerprint.
     *
     * @param s statement
     * @return SHA-256 in hexadecimal
     */
    public static String of(MemberStatement s) {
        StringBuilder text = new StringBuilder();
        text.append(s.memberId()).append('|').append(s.billingPeriod()).append('|').append(s.total()).append('|')
                .append(s.income()).append('\n');
        s.cards().forEach(g -> {
            text.append("C|").append(g.cardName()).append('|').append(g.total()).append('\n');
            g.lines().forEach(l -> text.append(l.date()).append('|').append(l.description()).append('|')
                    .append(l.installment()).append('|').append(l.amount()).append('|').append(l.percent()).append('|')
                    .append(l.category()).append('\n'));
        });
        s.fixedExpenses().forEach(e -> text.append("F|").append(e.description()).append('|').append(e.amount()).append('\n'));
        s.variableExpenses().forEach(e -> text.append("V|").append(e.date()).append('|').append(e.description()).append('|')
                .append(e.amount()).append('\n'));
        s.committed().forEach(c -> text.append("P|").append(c.billingPeriod()).append('|').append(c.amount()).append('\n'));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
