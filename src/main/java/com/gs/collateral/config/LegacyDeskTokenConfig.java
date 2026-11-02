package com.gs.collateral.config;

import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kill-switch for the legacy desk-token path. Only registered when
 * {@code collateral.auth.legacy-desk-token.enabled=true}; the default is off. Scheduled for removal
 * in the next release.
 */
@Configuration
@ConditionalOnProperty(prefix = "collateral.auth.legacy-desk-token", name = "enabled", havingValue = "true")
public class LegacyDeskTokenConfig {

    @Bean
    LegacyDeskTokenFilter legacyDeskTokenFilter(DeskTokenProperties deskTokenProperties, Clock clock) {
        return new LegacyDeskTokenFilter(deskTokenProperties, clock);
    }

    /** Keep the servlet container from registering the filter a second time outside the security chain. */
    @Bean
    FilterRegistrationBean<LegacyDeskTokenFilter> legacyDeskTokenFilterRegistration(LegacyDeskTokenFilter filter) {
        FilterRegistrationBean<LegacyDeskTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
