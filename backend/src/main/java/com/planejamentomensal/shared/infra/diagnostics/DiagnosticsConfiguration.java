package com.planejamentomensal.shared.infra.diagnostics;

import com.planejamentomensal.shared.application.DataFolders;
import com.planejamentomensal.shared.application.DebugModeStore;
import com.planejamentomensal.shared.application.LogLevels;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the adapters of the debug mode (RNF-11).
 *
 * @since 1.0
 */
@Configuration
public class DiagnosticsConfiguration {

    /**
     * Creates the configuration.
     */
    public DiagnosticsConfiguration() {
        // Only bean methods
    }

    /**
     * Keeps the debug mode in the data folder.
     *
     * @param folders data folders
     * @return the store
     */
    @Bean
    DebugModeStore debugModeStore(DataFolders folders) {
        return new FileDebugModeStore(folders.data());
    }

    /**
     * Changes log levels at runtime.
     *
     * @param logging logging system of the app
     * @return the adapter
     */
    @Bean
    LogLevels logLevels(LoggingSystem logging) {
        return new SpringLogLevels(logging);
    }
}
