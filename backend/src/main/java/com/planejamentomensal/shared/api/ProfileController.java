package com.planejamentomensal.shared.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Arrays;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HAL profile that documents the custom link relations of the API (API-10).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    /**
     * Description of one custom relation.
     *
     * @param name        relation name used in {@code _links}
     * @param description what following the relation does
     */
    public record RelationDescription(String name, String description) {
    }

    /**
     * Lists every custom relation.
     *
     * @return all relations, each with a {@code self} link
     */
    @GetMapping
    public CollectionModel<EntityModel<RelationDescription>> profile() {
        List<EntityModel<RelationDescription>> relations = Arrays.stream(Relations.values())
                .map(this::toModel)
                .toList();
        return CollectionModel.of(relations, linkTo(methodOn(ProfileController.class).profile()).withSelfRel());
    }

    /**
     * Describes one custom relation.
     *
     * @param name relation name, such as {@code rename}
     * @return the relation description
     * @throws NotFoundException if {@code name} is not a custom relation of this API
     */
    @GetMapping("/{name}")
    public EntityModel<RelationDescription> relation(@PathVariable String name) {
        return Relations.fromValue(name)
                .map(this::toModel)
                .orElseThrow(() -> new NotFoundException("relation", name));
    }

    private EntityModel<RelationDescription> toModel(Relations relation) {
        return EntityModel.of(new RelationDescription(relation.value(), relation.description()),
                linkTo(methodOn(ProfileController.class).relation(relation.value())).withSelfRel());
    }
}
