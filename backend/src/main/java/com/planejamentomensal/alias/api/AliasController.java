package com.planejamentomensal.alias.api;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.alias.application.AliasView;
import com.planejamentomensal.shared.api.RootLinkContributor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aliases and the descriptions they group (RF-46 to RF-49, RN-24).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/aliases")
public class AliasController {

    private final AliasService aliases;

    /**
     * Creates the controller.
     *
     * @param aliases alias service
     */
    public AliasController(AliasService aliases) {
        this.aliases = aliases;
    }

    /**
     * Description to group into an alias.
     *
     * @param originalDescription original description of the same store
     */
    public record PatternRequest(@NotBlank String originalDescription) {
    }

    /**
     * Lists aliases by name.
     *
     * @return aliases
     */
    @GetMapping
    public CollectionModel<AliasModel> aliases() {
        List<AliasModel> all = aliases.all().values().stream()
                .sorted(Comparator.comparing(AliasView::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(this::model).toList();
        return CollectionModel.of(all, Link.of("/api/aliases").withSelfRel());
    }

    /**
     * Groups another description of the same store into an alias (RN-24).
     *
     * @param id      alias id
     * @param request description
     * @return the alias
     */
    @PostMapping("/{id}/patterns")
    public AliasModel groupDescription(@PathVariable long id, @Valid @RequestBody PatternRequest request) {
        return model(aliases.addDescription(id, request.originalDescription()));
    }

    private AliasModel model(AliasView alias) {
        AliasModel model = new AliasModel(alias);
        model.add(Link.of("/api/aliases/" + alias.id() + "/patterns", "group"));
        return model;
    }

    /**
     * Adds {@code aliases} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor aliasesRootLink() {
        return () -> Link.of("/api/aliases", "aliases");
    }
}
