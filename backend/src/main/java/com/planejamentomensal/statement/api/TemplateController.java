package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.RenderedStatement;
import com.planejamentomensal.statement.application.StatementRequest;
import com.planejamentomensal.statement.application.StatementService;
import com.planejamentomensal.statement.application.TemplateService;
import com.planejamentomensal.statement.domain.StatementOptions;
import com.planejamentomensal.statement.domain.Template;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statement templates: settings without code, versions, the default one, per-member choice and live preview
 * (TPL-02 to TPL-07, IMP-36).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templates;
    private final StatementService statements;

    /**
     * Creates the controller.
     *
     * @param templates  templates
     * @param statements statement rendering
     */
    public TemplateController(TemplateService templates, StatementService statements) {
        this.templates = templates;
        this.statements = statements;
    }

    /**
     * Lists the templates; the default one is created on first use.
     *
     * @return templates by name
     */
    @GetMapping
    public CollectionModel<TemplateModel> templates() {
        return CollectionModel.of(templates.all().stream().map(this::toModel).toList(),
                Link.of("/api/templates").withSelfRel());
    }

    /**
     * Returns one template with its version as {@code ETag}.
     *
     * @param id template
     * @return the template
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateModel> template(@PathVariable long id) {
        Template template = templates.get(id);
        return ETags.ok(toModel(template), template.version());
    }

    /**
     * Creates a template.
     *
     * @param request name and settings
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<TemplateModel> createTemplate(@Valid @RequestBody TemplateRequest request) {
        Template template = templates.create(request.name(), request.settings());
        TemplateModel model = toModel(template);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(template.version())).body(model);
    }

    /**
     * Changes a template; changed settings make a new version (TPL-06).
     *
     * @param id      template
     * @param request name and settings
     * @param ifMatch version being edited
     * @return the template
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateModel> replaceTemplate(@PathVariable long id, @Valid @RequestBody TemplateRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Template template = templates.update(id, request.name(), request.settings(), ETags.expectedVersion(ifMatch));
        return ETags.ok(toModel(template), template.version());
    }

    /**
     * Makes a template the default (TPL-04).
     *
     * @param id template
     * @return the template
     */
    @PostMapping("/{id}/default")
    public ResponseEntity<TemplateModel> makeTemplateDefault(@PathVariable long id) {
        Template template = templates.makeDefault(id);
        return ETags.ok(toModel(template), template.version());
    }

    /**
     * Deletes a template that is not the default.
     *
     * @param id template
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeTemplate(@PathVariable long id) {
        templates.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists the versions of a template, newest first (TPL-06).
     *
     * @param id template
     * @return versions
     */
    @GetMapping("/{id}/versions")
    public List<TemplateVersionModel> templateVersions(@PathVariable long id) {
        return templates.versions(id).stream().map(v -> new TemplateVersionModel(v.number(), v.createdAt())).toList();
    }

    /**
     * Renders unsaved settings with real or sample data (TPL-03).
     *
     * @param request settings, period, member and language
     * @return the rendering
     */
    @PostMapping("/preview")
    public TemplatePreviewModel previewTemplate(@Valid @RequestBody TemplatePreviewRequest request) {
        RenderedStatement r = statements.preview(new StatementRequest(request.memberId() == null ? 0 : request.memberId(),
                BillingPeriod.parse(request.billingPeriod()), StatementOptions.DEFAULT, request.language(), null, null),
                request.settings());
        return new TemplatePreviewModel(r.html(), r.subject(), r.emailText(), r.whatsappText(), r.fallback());
    }

    /**
     * Returns the template chosen for each member (TPL-04).
     *
     * @return template id by member id
     */
    @GetMapping("/assignments")
    public Map<Long, Long> templateAssignments() {
        return templates.assignments();
    }

    /**
     * Chooses the template of a member, or clears the choice.
     *
     * @param memberId member
     * @param request  template, or {@code null} for the default
     * @return 204
     */
    @PutMapping("/assignments/{memberId}")
    public ResponseEntity<Void> assignTemplate(@PathVariable long memberId, @RequestBody TemplateAssignmentRequest request) {
        templates.assign(memberId, request.templateId());
        return ResponseEntity.noContent().build();
    }

    private TemplateModel toModel(Template template) {
        TemplateModel model = new TemplateModel(template, templates.settings(template));
        String self = "/api/templates/" + template.id();
        model.add(Link.of(self).withSelfRel());
        model.add(Link.of("/api/templates/preview", Relations.PREVIEW));
        model.add(Link.of(self + "/versions", Relations.VERSIONS));
        if (!template.isDefault()) {
            model.add(Link.of(self + "/default", Relations.MAKE_DEFAULT));
            model.add(Link.of(self, Relations.DELETE));
        }
        return model;
    }

    /**
     * Adds {@code templates} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor templatesRootLink() {
        return () -> Link.of("/api/templates", "templates");
    }
}
