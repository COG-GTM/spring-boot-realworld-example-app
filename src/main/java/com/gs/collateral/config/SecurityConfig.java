package com.gs.collateral.config;

import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DeskTokenProperties deskTokenProperties;
    private final Clock clock;

    public SecurityConfig(DeskTokenProperties deskTokenProperties, Clock clock) {
        this.deskTokenProperties = deskTokenProperties;
        this.clock = clock;
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
                .addFilterBefore(new LegacyDeskTokenFilter(deskTokenProperties, clock),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
