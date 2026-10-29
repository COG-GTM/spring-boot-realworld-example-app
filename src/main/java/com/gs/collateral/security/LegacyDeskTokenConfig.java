package com.gs.collateral.security;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the legacy desk-token filter only while the kill-switch
 * {@code collateral.auth.legacy-desk-token.enabled=true} is set. Default off; the filter and this
 * config are removed in the next release.
 */
@Configuration
@ConditionalOnProperty(prefix = "collateral.auth.legacy-desk-token", name = "enabled", havingValue = "true")
public class LegacyDeskTokenConfig {

    @Bean
    LegacyDeskTokenFilter legacyDeskTokenFilter(DeskTokenProperties properties, Clock clock) {
        return new LegacyDeskTokenFilter(properties, clock);
    }

    /** The filter runs inside the Spring Security chain only, not as a plain servlet filter. */
    @Bean
    FilterRegistrationBean<LegacyDeskTokenFilter> legacyDeskTokenFilterRegistration(LegacyDeskTokenFilter filter) {
        FilterRegistrationBean<LegacyDeskTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
