package com.planejamentomensal.statement.infra;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Sends the PDF renderer's own log to the app log, warnings and errors only; at INFO it logs every font load.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class PdfLogging implements XRLogger {

    private static final Logger LOG = LoggerFactory.getLogger("com.openhtmltopdf");

    /**
     * Installs this logger in the PDF renderer.
     */
    public PdfLogging() {
        XRLog.setLoggerImpl(this);
    }

    @Override
    public void log(String where, Level level, String message) {
        log(where, level, message, null);
    }

    @Override
    public void log(String where, Level level, String message, Throwable error) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            LOG.error("{}: {}", where, message, error);
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            LOG.warn("{}: {}", where, message, error);
        }
    }

    @Override
    public void setLevel(String logger, Level level) {
        // levels come from the app log configuration
    }

    @Override
    public boolean isLogLevelEnabled(Diagnostic diagnostic) {
        return diagnostic.getLevel().intValue() >= Level.WARNING.intValue();
    }
}
