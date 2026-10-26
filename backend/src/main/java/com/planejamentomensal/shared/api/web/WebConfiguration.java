package com.planejamentomensal.shared.api.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * HTTP boundary of the local API: session token and the origins of the app window.
 *
 * <p>The Tauri window loads the interface from {@code tauri://localhost} (Linux) or
 * {@code http://tauri.localhost} (Windows); the Vite dev server uses {@code http://localhost:5173}
 * (IMP-04, POR-08).</p>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    /**
     * Creates the configuration.
     *
     * @param allowedOrigins origins allowed to call the API ({@code app.web.allowed-origins})
     */
    public WebConfiguration(@Value("${app.web.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Registers the session token filter before any other filter.
     *
     * @param token session token passed by the shell ({@code app.session-token})
     * @return the filter registration
     */
    @Bean
    public FilterRegistrationBean<SessionTokenFilter> sessionTokenFilter(@Value("${app.session-token:}") String token) {
        FilterRegistrationBean<SessionTokenFilter> registration = new FilterRegistrationBean<>(new SessionTokenFilter(token));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/api", "/api/*");
        return registration;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(HttpHeaders.ETAG, HttpHeaders.LOCATION);
    }
}
