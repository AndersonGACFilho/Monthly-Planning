package com.planejamentomensal.shared.infra.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Folds the WAL file back into the database when the app stops.
 *
 * <p>Implements POR-11: after closing, {@code data/} holds a single consistent database file, so the
 * external drive can move to another computer.</p>
 *
 * @since 1.0
 */
@Component
public class WalCheckpointOnShutdown implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(WalCheckpointOnShutdown.class);

    private final DataSource dataSource;
    private volatile boolean running;

    /**
     * Creates the shutdown hook.
     *
     * @param dataSource SQLite data source
     */
    public WalCheckpointOnShutdown(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void start() {
        running = true;
    }

    /**
     * Runs {@code PRAGMA wal_checkpoint(TRUNCATE)} before the connection pool closes.
     */
    @Override
    public void stop() {
        running = false;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        } catch (SQLException e) {
            LOG.warn("WAL checkpoint on shutdown failed", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops after the web server, before the data source is closed.
     *
     * @return a phase lower than the web server's
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE + 1000;
    }
}
