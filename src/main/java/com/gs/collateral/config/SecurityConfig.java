package com.gs.collateral.config;

import com.gs.collateral.security.DeskTokenProperties;
import com.gs.collateral.security.LegacyDeskTokenFilter;
import java.time.Clock;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final DeskTokenProperties deskTokenProperties;
    private final Clock clock;

    public SecurityConfig(DeskTokenProperties deskTokenProperties, Clock clock) {
        this.deskTokenProperties = deskTokenProperties;
        this.clock = clock;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .and()
            .authorizeRequests()
                .antMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .antMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .antMatchers("/api/margin-calls/**").hasRole("OPS")
                .antMatchers("/api/agreements/**").hasAnyRole("OPS", "RISK", "TREASURY")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(new LegacyDeskTokenFilter(deskTokenProperties, clock),
                    UsernamePasswordAuthenticationFilter.class);
    }
}
