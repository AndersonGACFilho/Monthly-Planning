package com.planejamentomensal.shared.application;

import com.planejamentomensal.shared.domain.AppSetting;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of settings (SOL-04).
 *
 * @since 1.0
 */
public interface SettingRepository {

    /**
     * Finds a stored setting.
     *
     * @param key setting key
     * @return the stored setting, or empty when it still has its default
     */
    Optional<AppSetting> findById(String key);

    /**
     * Lists every stored setting.
     *
     * @return stored settings
     */
    List<AppSetting> findAll();

    /**
     * Stores a setting.
     *
     * @param setting setting to store
     * @return the stored setting
     */
    AppSetting save(AppSetting setting);
}
