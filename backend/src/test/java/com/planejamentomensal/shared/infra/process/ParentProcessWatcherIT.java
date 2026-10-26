package com.planejamentomensal.shared.infra.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the packaged backend in a child JVM whose "shell" is a short-lived process.
 */
class ParentProcessWatcherIT {

    @TempDir
    Path home;

    @Test
    @DisplayName("POR-08: backend stops cleanly when the shell process ends")
    void stopsWhenParentEnds() throws Exception {
        // Given a fake shell that lives for a few seconds
        Process shell = new ProcessBuilder(javaBin(), "-version").start();
        long shellPid = shell.pid();
        shell.waitFor(10, TimeUnit.SECONDS);
        // When the backend starts with that (already gone) parent PID
        Process backend = new ProcessBuilder(javaBin(), "-cp", System.getProperty("java.class.path"),
                "com.planejamentomensal.PlanejamentoMensalApplication",
                "--app.home=" + home, "--server.port=0", "--app.parent-pid=" + shellPid)
                .redirectErrorStream(true)
                .redirectOutput(home.resolve("backend.log").toFile())
                .start();
        // Then it shuts itself down, releasing the lock
        boolean ended = backend.waitFor(90, TimeUnit.SECONDS);
        if (!ended) {
            backend.destroyForcibly();
        }
        assertThat(ended).as("backend ended; log:%n%s", Files.readString(home.resolve("backend.log"))).isTrue();
        assertThat(backend.exitValue()).isZero();
        assertThat(home.resolve("data/instance.lock")).doesNotExist();
    }

    private static String javaBin() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString() + (File.separatorChar == '\\' ? ".exe" : "");
    }
}
