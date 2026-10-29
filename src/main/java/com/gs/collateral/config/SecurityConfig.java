package com.gs.collateral.config;

import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.GsAuthProperties;
import com.gs.collateral.security.GsEntitlementsConverter;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * GS Auth: requests carry a GS SSO bearer JWT, validated as an OAuth2 resource server; the
 * {@code gs_entitlements} claim is mapped to roles by {@link GsEntitlementsConverter}. The legacy
 * desk-token filter stays available behind {@code collateral.auth.legacy-desk-token.enabled} for
 * one release.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GsAuthProperties gsAuthProperties;

    public SecurityConfig(GsAuthProperties gsAuthProperties) {
        this.gsAuthProperties = gsAuthProperties;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<LegacyDeskTokenFilter> legacyFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/margin-calls/**").hasRole("OPS")
                        .requestMatchers("/api/agreements/**").hasAnyRole("OPS", "RISK", "TREASURY")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new GsEntitlementsConverter(gsAuthProperties))));
        legacyFilter.ifAvailable(filter -> http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        return http.build();
    }

    /** Issuer metadata is fetched lazily on the first token, so the service boots without reaching GS SSO. */
    @Bean
    JwtDecoder jwtDecoder() {
        String issuer = gsAuthProperties.getIssuerUri();
        String audience = gsAuthProperties.getAudience();
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>("aud",
                    aud -> aud != null && aud.contains(audience));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
            return decoder;
        });
    }

    /** Legacy desk tokens: kill-switch, off by default; removal is a follow-up release. */
    @Configuration
    @ConditionalOnProperty(prefix = "collateral.auth.legacy-desk-token", name = "enabled", havingValue = "true")
    static class LegacyDeskTokenConfig {

        @Bean
        LegacyDeskTokenFilter legacyDeskTokenFilter(DeskTokenProperties deskTokenProperties, Clock clock) {
            return new LegacyDeskTokenFilter(deskTokenProperties, clock);
        }

        /** Keep the filter inside the security chain only; stop the servlet container registering it too. */
        @Bean
        FilterRegistrationBean<LegacyDeskTokenFilter> legacyDeskTokenFilterRegistration(LegacyDeskTokenFilter filter) {
            FilterRegistrationBean<LegacyDeskTokenFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }
    }
}
