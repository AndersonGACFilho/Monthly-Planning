package com.planejamentomensal.shared.infra.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock used for issue dates and timestamps, replaceable in tests.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    /**
     * Creates the configuration.
     */
    public ClockConfiguration() {
        // configuration holder
    }

    /**
     * Returns the system clock in the local time zone.
     *
     * @return clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
