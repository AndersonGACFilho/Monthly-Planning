package com.planejamentomensal.statement.application;

import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.TemplateSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds, renders and converts statements for preview and download (RF-84, RF-85, TPL-03).
 *
 * @since 1.0
 */
@Service
@Transactional
public class StatementService {

    private final MemberStatementBuilder builder;
    private final TemplateService templates;
    private final TemplateRenderer renderer;
    private final PdfGenerator pdf;
    private final ImageGenerator images;
    private final StatementSpreadsheet spreadsheet;
    private final MemberQueries members;

    /**
     * Creates the service.
     *
     * @param builder     statement builder
     * @param templates   templates
     * @param renderer    HTML renderer
     * @param pdf         PDF generator
     * @param images      PNG generator
     * @param spreadsheet XLSX and CSV writer
     * @param members     member queries
     */
    public StatementService(MemberStatementBuilder builder, TemplateService templates, TemplateRenderer renderer,
            PdfGenerator pdf, ImageGenerator images, StatementSpreadsheet spreadsheet, MemberQueries members) {
        this.builder = builder;
        this.templates = templates;
        this.renderer = renderer;
        this.pdf = pdf;
        this.images = images;
        this.spreadsheet = spreadsheet;
        this.members = members;
    }

    /**
     * Builds and renders a statement in the member's language unless another is asked (I18N-08, I18N-09).
     *
     * @param request what to put in the statement
     * @return the prepared statement
     */
    public PreparedStatement prepare(StatementRequest request) {
        MemberView member = members.find(request.memberId())
                .orElseThrow(() -> new NotFoundException("member", request.memberId()));
        MemberStatement statement = builder.build(request.memberId(), request.period(), request.options());
        ResolvedTemplate template = templates.resolve(request.memberId(), request.templateId());
        String language = request.language() != null ? request.language() : member.preferredLanguage();
        RenderedStatement rendered = renderer.render(statement, template.settings(), language, request.note());
        return new PreparedStatement(statement, template, rendered, member.email(), member.whatsappPhone());
    }

    /**
     * Renders a statement with unsaved settings, for the live preview of the template editor (TPL-03).
     *
     * @param request  what to put in the statement; member 0 uses {@link SampleStatement}
     * @param settings settings being edited
     * @return the rendering
     */
    public RenderedStatement preview(StatementRequest request, TemplateSettings settings) {
        settings.validate();
        MemberStatement statement = request.memberId() == 0 ? SampleStatement.of(request.period())
                : builder.build(request.memberId(), request.period(), request.options());
        String language = request.language() != null ? request.language()
                : members.find(request.memberId()).map(MemberView::preferredLanguage).orElse("pt-BR");
        return renderer.render(statement, settings, language, request.note());
    }

    /**
     * Converts a prepared statement into PDF (RF-84).
     *
     * @param prepared statement
     * @return PDF bytes
     */
    public byte[] pdf(PreparedStatement prepared) {
        return pdf.toPdf(prepared.rendered().html());
    }

    /**
     * Converts a prepared statement into PNG (RF-84).
     *
     * @param prepared statement
     * @return PNG bytes
     */
    public byte[] png(PreparedStatement prepared) {
        return images.toPng(pdf(prepared));
    }

    /**
     * Writes a prepared statement as XLSX (RF-84).
     *
     * @param prepared statement
     * @return XLSX bytes
     */
    public byte[] xlsx(PreparedStatement prepared) {
        return spreadsheet.toXlsx(prepared.statement(), prepared.rendered().language());
    }

    /**
     * Writes a prepared statement as CSV (RF-84).
     *
     * @param prepared statement
     * @return CSV bytes
     */
    public byte[] csv(PreparedStatement prepared) {
        return spreadsheet.toCsv(prepared.statement(), prepared.rendered().language());
    }
}
