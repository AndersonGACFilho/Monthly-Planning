package com.planejamentomensal.shared.application;

import com.planejamentomensal.shared.domain.AppSetting;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and changes user settings with typed accessors (IMP-10).
 *
 * @since 1.0
 */
@Service
public class Settings {

    private final SettingRepository repository;

    /**
     * Creates the service.
     *
     * @param repository settings storage
     */
    public Settings(SettingRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a setting as text, or its default.
     *
     * @param setting setting
     * @return value as text
     */
    @Transactional(readOnly = true)
    public String get(Setting setting) {
        return repository.findById(setting.key()).map(AppSetting::value).orElse(setting.defaultValue());
    }

    /**
     * Returns a whole-number setting.
     *
     * @param setting setting holding a number
     * @return value
     */
    public int getInt(Setting setting) {
        return Integer.parseInt(get(setting));
    }

    /**
     * Returns a yes/no setting.
     *
     * @param setting setting holding {@code true} or {@code false}
     * @return value
     */
    public boolean getBoolean(Setting setting) {
        return Boolean.parseBoolean(get(setting));
    }

    /**
     * Returns a list of ids stored as comma-separated text.
     *
     * @param setting setting holding ids
     * @return ids in stored order, empty if none
     */
    public List<Long> getIds(Setting setting) {
        String value = get(setting);
        return value.isEmpty() ? List.of() : Arrays.stream(value.split(",")).map(Long::valueOf).toList();
    }

    /**
     * Returns every setting with its current value.
     *
     * @return map from key to value, in declaration order
     */
    @Transactional(readOnly = true)
    public Map<String, String> all() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Setting setting : Setting.values()) {
            values.put(setting.key(), setting.defaultValue());
        }
        repository.findAll().forEach(stored -> values.computeIfPresent(stored.key(), (k, v) -> stored.value()));
        return values;
    }

    /**
     * Changes several settings at once; nothing changes if one is invalid.
     *
     * @param changes map from key to new value
     * @throws BusinessRuleException with code {@code invalid-setting} for an unknown key or invalid value
     */
    @Transactional
    public void change(Map<String, String> changes) {
        changes.forEach((key, value) -> {
            Setting setting = Setting.fromKey(key).orElseThrow(() -> new BusinessRuleException("invalid-setting", key));
            if (!setting.isValid(value)) {
                throw new BusinessRuleException("invalid-setting", key);
            }
        });
        changes.forEach((key, value) -> repository.findById(key)
                .ifPresentOrElse(stored -> stored.change(value), () -> repository.save(new AppSetting(key, value))));
    }
}
