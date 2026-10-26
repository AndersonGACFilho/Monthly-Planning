package com.planejamentomensal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the backend sidecar.
 *
 * <p>The desktop shell starts this application on 127.0.0.1 with a free port and a session token
 * (POR-08).</p>
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
        SpringApplication.run(PlanejamentoMensalApplication.class, args);
    }
}
