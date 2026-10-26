package com.planejamentomensal;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the backend sidecar.
 *
 * <p>The desktop shell starts this application on 127.0.0.1 with a free port and a session token
 * (POR-08). Startup failures that carry an exit code (such as a locked database, POR-10) end the process
 * with that code so the shell can react.</p>
 *
 * @since 1.0
 */
@SpringBootApplication
public class PlanejamentoMensalApplication {

    /**
     * Starts the Spring application.
     *
     * @param args command-line arguments, such as {@code --server.port=<port>}
     */
    public static void main(String[] args) {
        try {
            SpringApplication.run(PlanejamentoMensalApplication.class, args);
        } catch (RuntimeException e) {
            int exitCode = exitCodeOf(e);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            throw e;
        }
    }

    /**
     * Finds the exit code carried by a startup failure.
     *
     * @param failure exception thrown while starting
     * @return the code of the first {@link ExitCodeGenerator} in the cause chain, or {@code 0} if none
     */
    static int exitCodeOf(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ExitCodeGenerator generator) {
                return generator.getExitCode();
            }
        }
        return 0;
    }
}
