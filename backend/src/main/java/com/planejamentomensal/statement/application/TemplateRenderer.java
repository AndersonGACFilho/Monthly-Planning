package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.FileNameSanitizer;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.MessageVariables;
import com.planejamentomensal.statement.domain.StatementBlock;
import com.planejamentomensal.statement.domain.StatementColumn;
import com.planejamentomensal.statement.domain.StatementCustomization;
import com.planejamentomensal.statement.domain.TemplateSettings;
import com.planejamentomensal.statement.domain.TemplateTexts;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Applies a template, a statement and the member's language and returns the HTML used for the e-mail, the PDF
 * and the PNG, plus the texts of the messages (TPL-01, TPL-05, TPL-07, I18N-08, I18N-10, I18N-11).
 *
 * <p>Fixed labels come from the {@code MessageSource} in the statement language; money and dates follow
 * {@link StatementFormat}.</p>
 *
 * @since 1.0
 */
@Component
public class TemplateRenderer {

    private static final List<String> LABELS = List.of("title", "member", "period", "issuedOn", "summary", "totalDue",
            "income", "balance", "cards", "date", "description", "installment", "amount", "share", "category", "subtotal",
            "dueDate", "fixedExpenses", "variableExpenses", "committed", "total", "payment", "pixKey", "note",
            "pendingWarning", "empty", "fallbackWarning", "previousBalance", "carriedOut", "subscriptions");

    private final TemplateEngine engine;
    private final MessageSource messages;

    /**
     * Creates the renderer.
     *
     * @param messages fixed labels
     */
    public TemplateRenderer(MessageSource messages) {
        this.messages = messages;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("statement/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /**
     * Renders a statement.
     *
     * @param statement statement
     * @param settings  template settings
     * @param language  language tag of the delivery
     * @param note      free note of this export, or {@code null} for the template's default message (RF-82)
     * @return HTML and message texts
     */
    public RenderedStatement render(MemberStatement statement, TemplateSettings settings, String language, String note) {
        return render(statement, settings, language, note, StatementCustomization.NONE);
    }

    /**
     * Renders a statement with the changes of one export (IMP-60): header, footer, blocks and hidden columns.
     *
     * @param statement     statement
     * @param template      settings of the template in use
     * @param language      language tag of the delivery
     * @param note          free note of this export, or {@code null} for the template's default message (RF-82)
     * @param customization changes of this export
     * @return HTML and message texts
     */
    public RenderedStatement render(MemberStatement statement, TemplateSettings template, String language, String note,
            StatementCustomization customization) {
        TemplateSettings settings = customization.applyTo(template);
        StatementFormat format = StatementFormat.of(language);
        Locale locale = format.locale();
        TemplateSettings.LocalizedTexts localized = settings.textsFor(format.language());
        TemplateTexts texts = localized.texts();
        Map<String, String> variables = variables(statement, format);

        Map<String, String> labels = new HashMap<>();
        LABELS.forEach(key -> labels.put(key, messages.getMessage("statement." + key, null, locale)));

        Context context = new Context(locale);
        context.setVariable("s", statement);
        context.setVariable("f", format);
        context.setVariable("labels", labels);
        context.setVariable("settings", settings);
        context.setVariable("fontFamily", settings.font().family());
        context.setVariable("blocks", settings.blocks().stream().map(StatementBlock::name).toList());
        context.setVariable("columns", columns(customization));
        context.setVariable("cardColumns", cardColumns(customization, statement.options().showCategories()));
        context.setVariable("variableColumns", variableColumns(customization, statement.options().showCategories()));
        context.setVariable("header", MessageVariables.apply(texts.header(), variables));
        context.setVariable("footer", MessageVariables.apply(texts.footer(), variables));
        context.setVariable("message", note != null && !note.isBlank() ? note.strip()
                : MessageVariables.apply(texts.message(), variables));
        context.setVariable("advancedCss", TemplateSanitizer.css(settings.advancedCss()));
        context.setVariable("advancedHtml", MessageVariables.apply(TemplateSanitizer.html(settings.advancedHtml()), escaped(variables)));
        context.setVariable("fallback", localized.fallback());
        context.setVariable("language", format.language());
        String standard = engine.process("statement", context);
        String html = settings.hasLayout() ? LayoutComposer.compose(standard, settings.layoutHtml(), escaped(variables)) : standard;

        String baseName = FileNameSanitizer.baseName(messages.getMessage("statement.file", null, locale) + " "
                + statement.memberCode() + " " + statement.billingPeriod());
        return new RenderedStatement(html, MessageVariables.apply(texts.emailSubject(), variables),
                MessageVariables.apply(texts.emailBody(), variables), MessageVariables.apply(texts.whatsapp(), variables),
                format.language(), localized.fallback(), baseName);
    }

    private static Map<String, Boolean> columns(StatementCustomization customization) {
        Map<String, Boolean> shown = new HashMap<>();
        for (StatementColumn column : StatementColumn.values()) {
            shown.put(column.name(), customization.shows(column));
        }
        return shown;
    }

    /** Columns of the card table: date, description, installment, category, share and amount, minus hidden ones. */
    private static int cardColumns(StatementCustomization customization, boolean showCategories) {
        return 2 + (showCategories ? 1 : 0) + shown(customization, StatementColumn.DATE)
                + shown(customization, StatementColumn.INSTALLMENT) + shown(customization, StatementColumn.SHARE);
    }

    /** Columns of the variable expenses table: date, description, category and amount, minus hidden ones. */
    private static int variableColumns(StatementCustomization customization, boolean showCategories) {
        return 2 + (showCategories ? 1 : 0) + shown(customization, StatementColumn.DATE);
    }

    private static int shown(StatementCustomization customization, StatementColumn column) {
        return customization.shows(column) ? 1 : 0;
    }

    private static Map<String, String> variables(MemberStatement s, StatementFormat format) {
        Map<String, String> values = new HashMap<>();
        values.put("member.name", s.memberName());
        values.put("billingPeriod", format.period(s.billingPeriod()));
        values.put("total", format.money(s.total()));
        values.put("dueDate", format.date(s.dueDate()));
        values.put("pixKey", s.pixKey() == null ? "" : s.pixKey());
        return values;
    }

    private static Map<String, String> escaped(Map<String, String> values) {
        Map<String, String> result = new HashMap<>();
        values.forEach((k, v) -> result.put(k, org.jsoup.nodes.Entities.escape(v)));
        return result;
    }
}
