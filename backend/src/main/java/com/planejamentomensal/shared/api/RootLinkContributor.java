package com.planejamentomensal.shared.api;

import org.springframework.hateoas.Link;

/**
 * Contributes the link of one top-level resource to the API entry point.
 *
 * <p>Each module registers its own contributor as a bean, so a new resource never edits
 * {@link RootController} (API-07, SOL-02).</p>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface RootLinkContributor {

    /**
     * Returns the link to the module's top-level resource.
     *
     * @return link whose relation is the resource name, such as {@code members}
     */
    Link rootLink();
}
