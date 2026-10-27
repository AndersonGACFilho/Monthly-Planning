package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.domain.Template;
import com.planejamentomensal.statement.domain.TemplateSettings;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a template (TPL-04, IMP-36).
 *
 * @since 1.0
 */
@Relation(itemRelation = "template", collectionRelation = "templates")
public class TemplateModel extends RepresentationModel<TemplateModel> {

    /** Id. */
    public final long id;
    /** Name. */
    public final String name;
    /** Default flag. */
    public final boolean isDefault;
    /** Current version number. */
    public final int currentVersion;
    /** Time of the last change. */
    public final Instant updatedAt;
    /** Settings. */
    public final TemplateSettings settings;
    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies a template.
     *
     * @param template template
     * @param settings its settings
     */
    public TemplateModel(Template template, TemplateSettings settings) {
        this.id = template.id();
        this.name = template.name();
        this.isDefault = template.isDefault();
        this.currentVersion = template.currentVersion();
        this.updatedAt = template.updatedAt();
        this.settings = settings;
        this.version = template.version();
    }
}
