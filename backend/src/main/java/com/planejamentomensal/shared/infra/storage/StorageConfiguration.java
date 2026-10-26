package com.planejamentomensal.shared.infra.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the portable app folder and the SQLite database inside it.
 *
 * <p>Implements POR-03 (database under {@code data/}) and POR-11 (WAL journal with
 * {@code synchronous=FULL}).</p>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class StorageConfiguration {

    /** Connections kept open; SQLite serializes writes, WAL lets reads run alongside. */
    private static final int MAX_CONNECTIONS = 4;

    /**
     * Creates the app paths and the {@code data/} folders.
     *
     * @param home app folder passed by the shell as {@code app.home}
     * @return the resolved paths
     */
    @Bean
    public AppPaths appPaths(@Value("${app.home}") Path home) {
        AppPaths paths = new AppPaths(home);
        paths.createDirectories();
        return paths;
    }

    /**
     * Creates the connection pool for the SQLite file in {@code data/}.
     *
     * @param paths app paths
     * @return pooled data source with the POR-11 pragmas set on every connection
     */
    @Bean
    public DataSource dataSource(AppPaths paths) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("sqlite");
        config.setJdbcUrl("jdbc:sqlite:" + paths.database());
        config.setMaximumPoolSize(MAX_CONNECTIONS);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "FULL");
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("busy_timeout", "10000");
        return new HikariDataSource(config);
    }
}
