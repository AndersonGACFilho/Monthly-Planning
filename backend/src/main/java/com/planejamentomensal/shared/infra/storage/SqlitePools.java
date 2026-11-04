package com.planejamentomensal.shared.infra.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;

/**
 * Connection pools for SQLite files with the POR-11 pragmas: WAL journal, {@code synchronous=FULL}, foreign keys
 * and transactions that start with {@code BEGIN IMMEDIATE} so writers queue.
 *
 * @since 1.0
 */
public final class SqlitePools {

    /** Pool name of the live database. */
    public static final String LIVE = "sqlite";

    /** Connections kept open; SQLite serializes writes, WAL lets reads run alongside. */
    private static final int MAX_CONNECTIONS = 4;

    private SqlitePools() {
    }

    /**
     * Opens a pool for a database file.
     *
     * @param database SQLite file, created when missing
     * @param name     pool name shown in the logs
     * @return the pool
     */
    public static HikariDataSource open(Path database, String name) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(name);
        config.setJdbcUrl("jdbc:sqlite:" + database);
        config.setMaximumPoolSize(MAX_CONNECTIONS);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "FULL");
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("busy_timeout", "10000");
        // BEGIN IMMEDIATE: concurrent writers wait for the lock instead of failing with SQLITE_BUSY when a
        // read transaction is upgraded to a write
        config.addDataSourceProperty("transaction_mode", "IMMEDIATE");
        return new HikariDataSource(config);
    }
}
