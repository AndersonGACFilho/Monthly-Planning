package com.planejamentomensal.shared.infra.diagnostics;

import com.planejamentomensal.shared.application.LogLevels;
import java.util.List;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

/**
 * Changes log levels through Spring Boot's logging system (RNF-11): the app and the web layer go to DEBUG; turning the
 * mode off gives them back the levels of the configuration.
 *
 * @since 1.0
 */
public class SpringLogLevels implements LogLevels {

    private static final List<String> DEBUG_LOGGERS = List.of("com.planejamentomensal", "org.springframework.web");

    private final LoggingSystem logging;

    /**
     * Creates the adapter.
     *
     * @param logging logging system of the app
     */
    public SpringLogLevels(LoggingSystem logging) {
        this.logging = logging;
    }

    @Override
    public void apply(boolean debug) {
        DEBUG_LOGGERS.forEach(logger -> logging.setLogLevel(logger, debug ? LogLevel.DEBUG : null));
    }
}
