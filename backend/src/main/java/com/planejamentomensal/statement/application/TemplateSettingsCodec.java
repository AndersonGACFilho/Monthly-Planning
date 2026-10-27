package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.TemplateSettings;

/**
 * Converts template settings to and from the JSON kept in the database (TPL-06).
 *
 * @since 1.0
 */
public interface TemplateSettingsCodec {

    /**
     * Writes settings as JSON.
     *
     * @param settings settings
     * @return JSON
     */
    String toJson(TemplateSettings settings);

    /**
     * Reads settings from JSON.
     *
     * @param json JSON
     * @return settings
     */
    TemplateSettings fromJson(String json);
}
