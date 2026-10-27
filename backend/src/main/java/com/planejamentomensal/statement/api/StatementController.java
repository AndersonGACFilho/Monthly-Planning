package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.PreparedStatement;
import com.planejamentomensal.statement.application.StatementRequest;
import com.planejamentomensal.statement.application.StatementService;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member statements of a billing period: preview and downloads as HTML, PDF, PNG, XLSX and CSV (RF-81 to RF-85,
 * I18N-09).
 *
 * @since 1.0
 */
@RestController
public class StatementController {

    private static final String BASE = "/api/statements/{billingPeriod}/members/{memberId}";
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StatementService service;

    /**
     * Creates the controller.
     *
     * @param service statement service
     */
    public StatementController(StatementService service) {
        this.service = service;
    }

    /**
     * Returns the statement of a member with links to its formats and to the channels the member can receive.
     *
     * @param billingPeriod  period, {@code yyyy-MM}
     * @param memberId       member
     * @param showCategories show categories (RF-83)
     * @param useAlias       use the displayed description (RF-83)
     * @param includeFuture  include future installments (RF-83)
     * @param language       language of this export, optional (I18N-09)
     * @param template       template, optional (TPL-04)
     * @param note           free note, optional (RF-82)
     * @return the statement
     */
    @GetMapping(BASE)
    public StatementModel memberStatement(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template, @RequestParam(required = false) String note) {
        PreparedStatement p = prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, note);
        StatementModel model = new StatementModel(p);
        String base = "/api/statements/" + p.statement().billingPeriod() + "/members/" + memberId;
        String query = query(showCategories, useAlias, includeFuture, language, template, note);
        model.add(Link.of(base + query).withSelfRel());
        model.add(Link.of(base + "/html" + query, Relations.PREVIEW));
        model.add(Link.of(base + "/pdf" + query, Relations.PDF));
        model.add(Link.of(base + "/png" + query, Relations.PNG));
        model.add(Link.of(base + "/xlsx" + query, Relations.XLSX));
        model.add(Link.of(base + "/csv" + query, Relations.CSV));
        model.add(Link.of("/api/statement-deliveries", Relations.SEND));
        if (p.email() != null && !p.email().isBlank()) {
            model.add(Link.of("/api/statement-deliveries", Relations.SEND_EMAIL));
        }
        if (p.phone() != null && !p.phone().isBlank()) {
            model.add(Link.of("/api/statement-deliveries", Relations.SEND_WHATSAPP));
        }
        return model;
    }

    /**
     * Returns the statement HTML for the preview (RF-85).
     *
     * @param billingPeriod  period
     * @param memberId       member
     * @param showCategories show categories
     * @param useAlias       use aliases
     * @param includeFuture  include future installments
     * @param language       language, optional
     * @param template       template, optional
     * @param note           free note, optional
     * @return HTML
     */
    @GetMapping(value = BASE + "/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> statementHtml(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template, @RequestParam(required = false) String note) {
        PreparedStatement p = prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, note);
        return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)).body(p.rendered().html());
    }

    /**
     * Downloads the statement as PDF (RF-84).
     *
     * @param billingPeriod  period
     * @param memberId       member
     * @param showCategories show categories
     * @param useAlias       use aliases
     * @param includeFuture  include future installments
     * @param language       language, optional
     * @param template       template, optional
     * @param note           free note, optional
     * @return PDF
     */
    @GetMapping(value = BASE + "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> statementPdf(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template, @RequestParam(required = false) String note) {
        return file(prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, note),
                "pdf", MediaType.APPLICATION_PDF, service::pdf);
    }

    /**
     * Downloads the statement as PNG (RF-84).
     *
     * @param billingPeriod  period
     * @param memberId       member
     * @param showCategories show categories
     * @param useAlias       use aliases
     * @param includeFuture  include future installments
     * @param language       language, optional
     * @param template       template, optional
     * @param note           free note, optional
     * @return PNG
     */
    @GetMapping(value = BASE + "/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> statementPng(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template, @RequestParam(required = false) String note) {
        return file(prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, note),
                "png", MediaType.IMAGE_PNG, service::png);
    }

    /**
     * Downloads the statement as XLSX (RF-84).
     *
     * @param billingPeriod  period
     * @param memberId       member
     * @param showCategories show categories
     * @param useAlias       use aliases
     * @param includeFuture  include future installments
     * @param language       language, optional
     * @param template       template, optional
     * @return XLSX
     */
    @GetMapping(value = BASE + "/xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> statementXlsx(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template) {
        return file(prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, null),
                "xlsx", XLSX, service::xlsx);
    }

    /**
     * Downloads the statement as CSV (RF-84).
     *
     * @param billingPeriod  period
     * @param memberId       member
     * @param showCategories show categories
     * @param useAlias       use aliases
     * @param includeFuture  include future installments
     * @param language       language, optional
     * @param template       template, optional
     * @return CSV
     */
    @GetMapping(value = BASE + "/csv", produces = "text/csv")
    public ResponseEntity<byte[]> statementCsv(@PathVariable String billingPeriod, @PathVariable long memberId,
            @RequestParam(defaultValue = "true") boolean showCategories, @RequestParam(defaultValue = "true") boolean useAlias,
            @RequestParam(defaultValue = "true") boolean includeFuture, @RequestParam(required = false) String language,
            @RequestParam(required = false) Long template) {
        return file(prepare(billingPeriod, memberId, showCategories, useAlias, includeFuture, language, template, null),
                "csv", new MediaType("text", "csv", StandardCharsets.UTF_8), service::csv);
    }

    private PreparedStatement prepare(String billingPeriod, long memberId, boolean showCategories, boolean useAlias,
            boolean includeFuture, String language, Long template, String note) {
        return service.prepare(new StatementRequest(memberId, BillingPeriod.parse(billingPeriod),
                new StatementOptions(showCategories, useAlias, includeFuture), blankToNull(language), template, note));
    }

    private static ResponseEntity<byte[]> file(PreparedStatement p, String extension, MediaType type,
            Function<PreparedStatement, byte[]> content) {
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(p.rendered().fileBaseName() + "." + extension).build().toString())
                .body(content.apply(p));
    }

    private static String query(boolean showCategories, boolean useAlias, boolean includeFuture, String language,
            Long template, String note) {
        StringBuilder q = new StringBuilder("?showCategories=" + showCategories + "&useAlias=" + useAlias
                + "&includeFuture=" + includeFuture);
        if (blankToNull(language) != null) {
            q.append("&language=").append(language);
        }
        if (template != null) {
            q.append("&template=").append(template);
        }
        if (note != null && !note.isBlank()) {
            q.append("&note=").append(URLEncoder.encode(note, StandardCharsets.UTF_8));
        }
        return q.toString();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * Adds {@code statements} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor statementsRootLink() {
        return () -> Link.of("/api/statements/{billingPeriod}/members/{memberId}", "statements");
    }
}
