package com.planejamentomensal.shared.infra.settings;

import com.planejamentomensal.shared.application.SettingRepository;
import com.planejamentomensal.shared.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link SettingRepository}.
 *
 * @since 1.0
 */
public interface JpaSettingRepository extends JpaRepository<AppSetting, String>, SettingRepository {
}
