package com.planejamentomensal.shared.api.openapi;

import com.planejamentomensal.shared.api.web.SessionTokenFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI description of the API, the single contract between backend and interface (API-12).
 *
 * <p>Operation descriptions come from controller Javadoc through therapi-runtime-javadoc (DOC-05). The
 * frontend generates its types, hooks and Zod schemas from this document with Orval.</p>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /** Name of the security scheme for the session token. */
    public static final String SESSION_TOKEN_SCHEME = "sessionToken";

    /**
     * Creates the configuration.
     */
    public OpenApiConfiguration() {
        // configuration holder
    }

    /**
     * Describes the API, its relative server and the session token header.
     *
     * @return OpenAPI root object completed by springdoc with the operations
     */
    @Bean
    public OpenAPI planejamentoMensalOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Planejamento Mensal API").version("v1")
                        .description("Local HAL API of the Planejamento Mensal desktop app. Start at GET /api."))
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes(SESSION_TOKEN_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(SessionTokenFilter.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_TOKEN_SCHEME));
    }
}
