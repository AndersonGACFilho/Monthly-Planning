package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.TemplateSettingsCodec;
import com.planejamentomensal.statement.domain.TemplateSettings;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson implementation of {@link TemplateSettingsCodec}.
 *
 * @since 1.0
 */
@Component
public class JacksonTemplateSettingsCodec implements TemplateSettingsCodec {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public String toJson(TemplateSettings settings) {
        return mapper.writeValueAsString(settings);
    }

    @Override
    public TemplateSettings fromJson(String json) {
        return mapper.readValue(json, TemplateSettings.class);
    }
}
