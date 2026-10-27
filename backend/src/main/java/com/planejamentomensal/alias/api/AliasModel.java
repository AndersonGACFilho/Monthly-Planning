package com.planejamentomensal.alias.api;

import com.planejamentomensal.alias.application.AliasView;
import java.util.Set;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of an alias (RF-46, RN-24).
 *
 * @since 1.0
 */
@Relation(itemRelation = "alias", collectionRelation = "aliases")
public class AliasModel extends RepresentationModel<AliasModel> {

    /** Id. */
    public final long id;
    /** Displayed name. */
    public final String displayName;
    /** Default category, or {@code null}. */
    public final Long defaultCategoryId;
    /** Normalized original descriptions grouped by the alias. */
    public final Set<String> patterns;

    /**
     * Copies an alias.
     *
     * @param alias alias
     */
    public AliasModel(AliasView alias) {
        this.id = alias.id();
        this.displayName = alias.displayName();
        this.defaultCategoryId = alias.defaultCategoryId();
        this.patterns = alias.patterns();
    }
}
