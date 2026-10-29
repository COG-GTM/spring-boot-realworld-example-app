package com.gs.collateral.config;

import com.gs.collateral.security.GsAuthProperties;
import com.gs.collateral.security.GsEntitlementsConverter;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
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
 * GS Auth: GS SSO JWTs validated as an OAuth2 resource server, entitlements mapped to roles by
 * {@link GsEntitlementsConverter}. The legacy desk-token filter is only present while
 * {@code collateral.auth.legacy-desk-token.enabled=true}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GsAuthProperties gsAuthProperties;
    private final ObjectProvider<LegacyDeskTokenFilter> legacyDeskTokenFilter;

    public SecurityConfig(GsAuthProperties gsAuthProperties, ObjectProvider<LegacyDeskTokenFilter> legacyDeskTokenFilter) {
        this.gsAuthProperties = gsAuthProperties;
        this.legacyDeskTokenFilter = legacyDeskTokenFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(gsEntitlementsConverter())));
        LegacyDeskTokenFilter legacy = legacyDeskTokenFilter.getIfAvailable();
        if (legacy != null) {
            http.addFilterBefore(legacy, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    GsEntitlementsConverter gsEntitlementsConverter() {
        return new GsEntitlementsConverter(gsAuthProperties);
    }

    /**
     * Issuer discovery is deferred to the first token so the service boots without reaching GS SSO
     * (same approach as Boot's own resource-server auto-configuration).
     */
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
}
