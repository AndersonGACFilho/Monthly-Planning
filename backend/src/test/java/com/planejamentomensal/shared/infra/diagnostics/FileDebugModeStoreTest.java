package com.planejamentomensal.shared.infra.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDebugModeStoreTest {

    @TempDir
    private Path data;

    @Test
    @DisplayName("RNF-11: the debug mode lives in data/debug-mode.json, outside the database, and starts off")
    void storesInDataFolder() {
        FileDebugModeStore store = new FileDebugModeStore(data);
        assertThat(store.load()).isFalse();
        store.save(true);
        assertThat(data.resolve("debug-mode.json")).exists();
        assertThat(new FileDebugModeStore(data).load()).isTrue();
        store.save(false);
        assertThat(store.load()).isFalse();
    }

    @Test
    @DisplayName("RNF-11: an unreadable file counts as off")
    void unreadableIsOff() throws IOException {
        Files.writeString(data.resolve("debug-mode.json"), "not json");
        assertThat(new FileDebugModeStore(data).load()).isFalse();
    }
}
