package com.planejamentomensal.shared.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.shared.application.Settings;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User settings (IMP-10): thresholds, auto-validation, spreadsheet and e-mail options, language.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final Settings settings;

    /**
     * Creates the controller.
     *
     * @param settings settings service
     */
    public SettingsController(Settings settings) {
        this.settings = settings;
    }

    /**
     * Returns every setting with its current value.
     *
     * @return settings keyed by name
     */
    @GetMapping
    public EntityModel<Map<String, String>> settings() {
        return EntityModel.of(settings.all(), linkTo(methodOn(SettingsController.class).settings()).withSelfRel());
    }

    /**
     * Changes some settings; keys left out keep their value.
     *
     * @param changes map from key to new value
     * @return every setting after the change
     */
    @PatchMapping
    public EntityModel<Map<String, String>> changeSettings(@RequestBody Map<String, String> changes) {
        settings.change(changes);
        return settings();
    }

    /**
     * Adds {@code settings} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor settingsRootLink() {
        return () -> Link.of("/api/settings", "settings");
    }
}
