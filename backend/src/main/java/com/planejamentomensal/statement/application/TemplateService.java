package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.Versions;
import com.planejamentomensal.statement.domain.MemberTemplate;
import com.planejamentomensal.statement.domain.StatementBlock;
import com.planejamentomensal.statement.domain.Template;
import com.planejamentomensal.statement.domain.TemplateFont;
import com.planejamentomensal.statement.domain.TemplateSettings;
import com.planejamentomensal.statement.domain.TemplateTexts;
import com.planejamentomensal.statement.domain.TemplateVersion;
import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved templates: one default, an optional template per member, and a frozen version for every change
 * (TPL-02, TPL-04, TPL-06).
 *
 * @since 1.0
 */
@Service
@Transactional
public class TemplateService {

    private final TemplateRepository templates;
    private final TemplateVersionRepository versions;
    private final MemberTemplateRepository memberTemplates;
    private final TemplateSettingsCodec codec;
    private final MessageSource messages;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param templates       template storage
     * @param versions        version storage
     * @param memberTemplates member choices
     * @param codec           settings JSON codec
     * @param messages        default texts
     * @param clock           clock
     */
    public TemplateService(TemplateRepository templates, TemplateVersionRepository versions,
            MemberTemplateRepository memberTemplates, TemplateSettingsCodec codec, MessageSource messages, Clock clock) {
        this.templates = templates;
        this.versions = versions;
        this.memberTemplates = memberTemplates;
        this.codec = codec;
        this.messages = messages;
        this.clock = clock;
    }

    /**
     * Lists the templates, creating the default one on first use.
     *
     * @return templates by name
     */
    public List<Template> all() {
        defaultTemplate();
        return templates.findAllByOrderByNameAsc();
    }

    /**
     * Loads a template.
     *
     * @param id id
     * @return the template
     */
    @Transactional(readOnly = true)
    public Template get(long id) {
        return templates.findById(id).orElseThrow(() -> new NotFoundException("template", id));
    }

    /**
     * Reads the settings of a template.
     *
     * @param template template
     * @return settings
     */
    public TemplateSettings settings(Template template) {
        return codec.fromJson(template.settingsJson());
    }

    /**
     * Creates a template at version 1; the first template becomes the default.
     *
     * @param name     name
     * @param settings settings
     * @return the template
     */
    public Template create(String name, TemplateSettings settings) {
        settings.validate();
        boolean first = templates.findFirstByDefaultTemplateTrue().isEmpty();
        Template template = new Template(name.strip(), codec.toJson(settings), clock.instant());
        template.setDefault(first);
        template = templates.saveAndFlush(template);
        versions.saveAndFlush(new TemplateVersion(template.id(), 1, template.settingsJson(), clock.instant()));
        return template;
    }

    /**
     * Changes a template; a settings change freezes a new version (TPL-06).
     *
     * @param id              template
     * @param name            name
     * @param settings        settings
     * @param expectedVersion version the client edited, or {@code null}
     * @return the template
     */
    public Template update(long id, String name, TemplateSettings settings, Long expectedVersion) {
        settings.validate();
        Template template = get(id);
        Versions.check(expectedVersion, template.version());
        if (template.change(name.strip(), codec.toJson(settings), clock.instant())) {
            versions.saveAndFlush(new TemplateVersion(template.id(), template.currentVersion(), template.settingsJson(),
                    clock.instant()));
        }
        return templates.saveAndFlush(template);
    }

    /**
     * Makes a template the default (TPL-04).
     *
     * @param id template
     * @return the template
     */
    public Template makeDefault(long id) {
        Template template = get(id);
        templates.findFirstByDefaultTemplateTrue().filter(t -> !t.id().equals(template.id())).ifPresent(old -> {
            old.setDefault(false);
            templates.saveAndFlush(old);
        });
        template.setDefault(true);
        return templates.saveAndFlush(template);
    }

    /**
     * Deletes a template that is not the default; members that used it go back to the default.
     *
     * @param id template
     * @throws BusinessRuleException with code {@code template-default-delete} for the default template
     */
    public void delete(long id) {
        Template template = get(id);
        if (template.isDefault()) {
            throw new BusinessRuleException("template-default-delete", id);
        }
        memberTemplates.findByTemplateId(id).forEach(memberTemplates::delete);
        templates.delete(template);
    }

    /**
     * Lists the versions of a template, newest first (TPL-06).
     *
     * @param id template
     * @return versions
     */
    @Transactional(readOnly = true)
    public List<TemplateVersion> versions(long id) {
        get(id);
        return versions.findByTemplateIdOrderByNumberDesc(id);
    }

    /**
     * Chooses the template of a member, or clears the choice (TPL-04).
     *
     * @param memberId   member
     * @param templateId template, or {@code null} to use the default
     */
    public void assign(long memberId, Long templateId) {
        Optional<MemberTemplate> current = memberTemplates.findById(memberId);
        if (templateId == null) {
            current.ifPresent(memberTemplates::delete);
            return;
        }
        get(templateId);
        MemberTemplate choice = current.orElseGet(() -> new MemberTemplate(memberId, templateId));
        choice.use(templateId);
        memberTemplates.saveAndFlush(choice);
    }

    /**
     * Returns the template chosen for each member.
     *
     * @return template id by member id
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> assignments() {
        Map<Long, Long> result = new LinkedHashMap<>();
        memberTemplates.findAll().forEach(c -> result.put(c.memberId(), c.templateId()));
        return result;
    }

    /**
     * Resolves the template of a statement: the given one, else the member's, else the default (TPL-04).
     *
     * @param memberId   member
     * @param templateId template asked for, or {@code null}
     * @return the template and its current version
     */
    public ResolvedTemplate resolve(long memberId, Long templateId) {
        Template template = templateId != null ? get(templateId)
                : memberTemplates.findById(memberId).flatMap(c -> templates.findById(c.templateId()))
                        .orElseGet(this::defaultTemplate);
        return new ResolvedTemplate(template.id(), template.currentVersion(), settings(template));
    }

    /**
     * Returns the default template, creating it from the default texts on first use.
     *
     * @return the default template
     */
    public Template defaultTemplate() {
        return templates.findFirstByDefaultTemplateTrue().orElseGet(() -> create(
                messages.getMessage("template.default.name", null, Locale.forLanguageTag("pt-BR")), defaultSettings()));
    }

    /**
     * Settings of the default template: blue and slate, Noto Sans, every block, texts in both languages.
     *
     * @return settings
     */
    public TemplateSettings defaultSettings() {
        Map<String, TemplateTexts> texts = new LinkedHashMap<>();
        for (String language : TemplateSettings.LANGUAGES) {
            Locale locale = Locale.forLanguageTag(language);
            texts.put(language, new TemplateTexts(text("header", locale), text("footer", locale), text("message", locale),
                    text("emailSubject", locale), text("emailBody", locale), text("whatsapp", locale)));
        }
        return new TemplateSettings("#2563EB", "#475569", TemplateFont.NOTO_SANS, null,
                Arrays.asList(StatementBlock.values()), texts, null, null);
    }

    private String text(String key, Locale locale) {
        return messages.getMessage("template.default." + key, null, locale);
    }
}
