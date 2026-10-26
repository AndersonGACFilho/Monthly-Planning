package com.planejamentomensal.shared.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppPathsTest {

    @TempDir
    Path home;

    @Test
    @DisplayName("POR-03: every app folder is inside the app home")
    void everyFolderIsInsideHome() {
        // Given
        AppPaths paths = new AppPaths(home);
        // When
        List<Path> folders = List.of(paths.data(), paths.database(), paths.templates(), paths.exports(),
                paths.backups(), paths.logs(), paths.temp());
        // Then
        assertThat(folders).allSatisfy(folder -> assertThat(folder).startsWithRaw(home));
        assertThat(paths.database()).isEqualTo(home.resolve("data/planejamento-mensal.db"));
    }

    @Test
    @DisplayName("POR-03: relative home is resolved to an absolute normalized path")
    void relativeHomeIsNormalized() {
        // Given / When
        AppPaths paths = new AppPaths(Path.of("some/../app"));
        // Then
        assertThat(paths.home()).isAbsolute().isEqualTo(Path.of("app").toAbsolutePath());
    }

    @Test
    @DisplayName("POR-03: data folders are created on demand")
    void createsDataFolders() {
        // Given
        AppPaths paths = new AppPaths(home);
        // When
        paths.createDirectories();
        // Then
        assertThat(List.of(paths.templates(), paths.exports(), paths.backups(), paths.logs(), paths.temp()))
                .allSatisfy(dir -> assertThat(Files.isDirectory(dir)).isTrue());
    }

    @Test
    @DisplayName("POR-03: paths escaping the app home are detected")
    void detectsPathsOutsideHome() {
        // Given
        AppPaths paths = new AppPaths(home);
        // When / Then
        assertThat(paths.contains(Path.of("data/exports/2026-11"))).isTrue();
        assertThat(paths.contains(Path.of("../outside"))).isFalse();
        assertThat(paths.contains(home.getRoot().resolve("etc"))).isFalse();
    }
}
