package com.planejamentomensal.i18n.api;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resolves the request language from {@code Accept-Language}, limited to the supported languages.
 *
 * <p>Implements I18N-06 (backend messages follow the interface language) for the languages of I18N-03.</p>
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class LocaleConfiguration {

    /** Portuguese (Brazil), the default language. */
    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    /** English (United States). */
    public static final Locale EN_US = Locale.forLanguageTag("en-US");

    /**
     * Creates the configuration.
     */
    public LocaleConfiguration() {
        // configuration holder
    }

    /**
     * Creates the locale resolver used by MVC, {@code ProblemDetail} and Bean Validation messages.
     *
     * @return resolver that accepts pt-BR and en-US and falls back to pt-BR
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(PT_BR, EN_US));
        resolver.setDefaultLocale(PT_BR);
        return resolver;
    }
}
