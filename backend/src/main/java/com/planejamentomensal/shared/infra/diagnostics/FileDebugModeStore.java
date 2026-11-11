package com.planejamentomensal.shared.infra.diagnostics;

import com.planejamentomensal.shared.application.DebugModeStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps the debug mode in {@code data/debug-mode.json} (RNF-11, POR-03).
 *
 * @since 1.0
 */
public class FileDebugModeStore implements DebugModeStore {

    private static final String FILE_NAME = "debug-mode.json";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Path file;

    /**
     * Creates the store.
     *
     * @param dataFolder the portable {@code data/} folder
     */
    public FileDebugModeStore(Path dataFolder) {
        this.file = dataFolder.resolve(FILE_NAME);
    }

    @Override
    public boolean load() {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            return mapper.readValue(file.toFile(), State.class).debugMode();
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public void save(boolean enabled) {
        try {
            Files.writeString(file, mapper.writeValueAsString(new State(enabled)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record State(boolean debugMode) {
    }
}
