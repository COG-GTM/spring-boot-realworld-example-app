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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GsAuthProperties gsAuthProperties;
    private final GsEntitlementsConverter entitlementsConverter;
    private final ObjectProvider<LegacyDeskTokenFilter> legacyDeskTokenFilter;

    public SecurityConfig(GsAuthProperties gsAuthProperties, GsEntitlementsConverter entitlementsConverter,
            ObjectProvider<LegacyDeskTokenFilter> legacyDeskTokenFilter) {
        this.gsAuthProperties = gsAuthProperties;
        this.entitlementsConverter = entitlementsConverter;
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
                        .requestMatchers("/api/margin-calls/*/dispute").hasAnyRole("OPS", "RISK")
                        .requestMatchers("/api/margin-calls/**").hasRole("OPS")
                        .requestMatchers("/api/agreements/**").hasAnyRole("OPS", "RISK", "TREASURY")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(entitlementsConverter)));
        LegacyDeskTokenFilter legacyFilter = legacyDeskTokenFilter.getIfAvailable();
        if (legacyFilter != null) {
            http.addFilterBefore(legacyFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    /**
     * Issuer + audience validated JWT decoder. Wrapped in a {@link SupplierJwtDecoder} so the OIDC
     * discovery call to the issuer happens on first use, not at startup.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(gsAuthProperties.getIssuerUri()).build();
            decoder.setJwtValidator(jwtValidator());
            return decoder;
        });
    }

    private OAuth2TokenValidator<Jwt> jwtValidator() {
        String audience = gsAuthProperties.getAudience();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(gsAuthProperties.getIssuerUri()), audienceValidator);
    }
}
