package com.planejamentomensal.shared.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Comparator;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API entry point: the only URL the interface knows (API-07).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api")
public class RootController {

    private final List<RootLinkContributor> contributors;

    /**
     * Creates the controller.
     *
     * @param contributors links of every top-level resource, one per module
     */
    public RootController(List<RootLinkContributor> contributors) {
        this.contributors = contributors;
    }

    /**
     * Returns links to every top-level resource and to the HAL profile.
     *
     * @return entry point with {@code self}, {@code profile} and one link per top-level resource
     */
    @GetMapping
    public RepresentationModel<?> root() {
        RepresentationModel<?> root = new RepresentationModel<>();
        root.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        root.add(linkTo(methodOn(ProfileController.class).profile()).withRel("profile"));
        contributors.stream()
                .map(RootLinkContributor::rootLink)
                .sorted(Comparator.comparing(link -> link.getRel().value()))
                .forEach(root::add);
        return root;
    }
}
