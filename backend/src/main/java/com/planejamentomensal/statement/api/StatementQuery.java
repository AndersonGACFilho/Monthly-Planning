package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.StatementRequest;
import com.planejamentomensal.statement.domain.StatementBlock;
import com.planejamentomensal.statement.domain.StatementColumn;
import com.planejamentomensal.statement.domain.StatementCustomization;
import com.planejamentomensal.statement.domain.StatementOptions;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query parameters of a statement and of each of its formats: export options (RF-83), language (I18N-09), template
 * (TPL-04), note (RF-82) and the changes of this export (IMP-60). Omitted options are {@code true}.
 *
 * @param showCategories show the category of each line
 * @param useAlias       show the displayed description instead of the original one
 * @param includeFuture  include the installments committed in the next periods
 * @param language       language of this export, or empty for the member's
 * @param template       template, or empty for the member's or the default
 * @param note           free note, or empty for the template's message
 * @param header         header text of this export, or empty for the template's
 * @param footer         footer text of this export, or empty for the template's
 * @param blocks         blocks shown, in order, or empty for the template's
 * @param hide           columns left out of the line tables
 * @since 1.0
 */
public record StatementQuery(Boolean showCategories, Boolean useAlias, Boolean includeFuture,
        @Pattern(regexp = "^(pt-BR|en-US)?$") String language, Long template, @Size(max = 1000) String note,
        @Size(max = 200) String header, @Size(max = 200) String footer, List<StatementBlock> blocks,
        List<StatementColumn> hide) {

    /**
     * Converts to the statement request of a member.
     *
     * @param memberId member
     * @param period   billing period, {@code yyyy-MM}
     * @return the request
     */
    StatementRequest toRequest(long memberId, String period) {
        return new StatementRequest(memberId, BillingPeriod.parse(period), options(), blankToNull(language), template,
                blankToNull(note), customization());
    }

    /**
     * Writes the parameters back as a query string for the links of the formats; only the given ones.
     *
     * @return query starting with {@code ?}
     */
    String toQueryString() {
        StatementOptions o = options();
        StringBuilder q = new StringBuilder("?showCategories=" + o.showCategories() + "&useAlias=" + o.useAlias()
                + "&includeFuture=" + o.includeFuture());
        append(q, "language", blankToNull(language));
        append(q, "template", template == null ? null : template.toString());
        append(q, "note", blankToNull(note));
        append(q, "header", blankToNull(header));
        append(q, "footer", blankToNull(footer));
        append(q, "blocks", blocks == null || blocks.isEmpty() ? null
                : blocks.stream().map(Enum::name).collect(Collectors.joining(",")));
        append(q, "hide", hide == null || hide.isEmpty() ? null
                : hide.stream().map(Enum::name).collect(Collectors.joining(",")));
        return q.toString();
    }

    private StatementOptions options() {
        return new StatementOptions(!Boolean.FALSE.equals(showCategories), !Boolean.FALSE.equals(useAlias),
                !Boolean.FALSE.equals(includeFuture));
    }

    private StatementCustomization customization() {
        return new StatementCustomization(header, footer, blocks,
                hide == null || hide.isEmpty() ? null : EnumSet.copyOf(hide));
    }

    private static void append(StringBuilder query, String name, String value) {
        if (value != null) {
            query.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
