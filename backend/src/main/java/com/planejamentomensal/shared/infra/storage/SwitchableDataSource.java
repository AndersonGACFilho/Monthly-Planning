package com.planejamentomensal.shared.infra.storage;

import com.zaxxer.hikari.HikariDataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Objects;
import org.springframework.jdbc.datasource.AbstractDataSource;

/**
 * Data source that hands out connections of one SQLite pool at a time and can change pools while the app runs, so
 * the user can open a backup and go back to the live database (IMP-58). While a pool is being replaced, connections
 * are refused with a transient error instead of reaching a file that is being moved.
 *
 * @since 1.0
 */
public class SwitchableDataSource extends AbstractDataSource implements Closeable {

    private volatile HikariDataSource current;

    /**
     * Creates the data source with its first pool.
     *
     * @param initial pool of the live database
     */
    public SwitchableDataSource(HikariDataSource initial) {
        this.current = Objects.requireNonNull(initial, "initial");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return pool().getConnection(username, password);
    }

    /**
     * Closes the current pool and refuses connections until {@link #resume(HikariDataSource)}.
     */
    public synchronized void suspend() {
        HikariDataSource old = current;
        current = null;
        if (old != null) {
            old.close();
        }
    }

    /**
     * Starts handing out connections of a pool, closing the previous one if still open.
     *
     * @param next pool to use from now on
     */
    public synchronized void resume(HikariDataSource next) {
        HikariDataSource old = current;
        current = Objects.requireNonNull(next, "next");
        if (old != null && old != next) {
            old.close();
        }
    }

    /**
     * Closes the current pool when the app stops.
     */
    @Override
    public synchronized void close() {
        suspend();
    }

    private HikariDataSource pool() throws SQLException {
        HikariDataSource pool = current;
        if (pool == null) {
            throw new SQLTransientConnectionException("The database is being switched; try again");
        }
        return pool;
    }
}
