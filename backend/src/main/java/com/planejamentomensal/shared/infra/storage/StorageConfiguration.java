package com.planejamentomensal.shared.infra.storage;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the portable app folder and the SQLite database inside it.
 *
 * <p>Implements POR-03 (database under {@code data/}) and POR-11 (WAL journal with
 * {@code synchronous=FULL}) through {@link SqlitePools}.</p>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class StorageConfiguration {

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
     * Takes the single-instance lock before anything opens the database (POR-10).
     *
     * @param paths app paths
     * @param force {@code app.lock.force}: the user confirmed taking over an orphaned lock
     * @return the acquired lock, released when the application stops
     */
    @Bean(destroyMethod = "release")
    public InstanceLock instanceLock(AppPaths paths, @Value("${app.lock.force:false}") boolean force) {
        InstanceLock lock = InstanceLock.forCurrentProcess(paths.data());
        lock.acquire(force);
        return lock;
    }

    /**
     * Creates the data source of the SQLite file in {@code data/}; it can later point at a backup (IMP-58).
     *
     * @param paths app paths
     * @param lock  single-instance lock; required so the database is only opened after it is held
     * @return switchable data source with the POR-11 pragmas set on every connection
     */
    @Bean(destroyMethod = "close")
    public SwitchableDataSource dataSource(AppPaths paths, InstanceLock lock) {
        return new SwitchableDataSource(SqlitePools.open(paths.database(), SqlitePools.LIVE));
    }
}
