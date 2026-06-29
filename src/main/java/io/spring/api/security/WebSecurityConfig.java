package io.spring.api.security;

import static java.util.Arrays.asList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Bean
  public JwtTokenFilter jwtTokenFilter() {
    return new JwtTokenFilter();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // CSRF protection is intentionally disabled.
    //
    // This API is fully stateless and authenticates every request with a JWT bearer
    // token carried in the "Authorization" header (see JwtTokenFilter), never with a
    // session cookie. Session creation is STATELESS and CORS is configured with
    // allowCredentials=false, so the browser never attaches ambient credentials
    // (cookies) to cross-site requests. CSRF attacks rely on such ambient credentials
    // being sent automatically; because authentication requires a token that an
    // attacker's site cannot read or forge, CSRF is not applicable here. Enabling CSRF
    // (e.g. CookieCsrfTokenRepository) would add a cookie/token exchange that this
    // token-based model does not use and would break the stateless API contract.
    http.csrf()
        .disable()
        .cors()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.OPTIONS)
        .permitAll()
        .antMatchers("/graphiql")
        .permitAll()
        .antMatchers("/graphql")
        .permitAll()
        .antMatchers(HttpMethod.GET, "/articles/feed")
        .authenticated()
        .antMatchers(HttpMethod.POST, "/users", "/users/login")
        .permitAll()
        .antMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags")
        .permitAll()
        .anyRequest()
        .authenticated();

    http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(asList("*"));
    configuration.setAllowedMethods(asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
    // setAllowCredentials(true) is important, otherwise:
    // The value of the 'Access-Control-Allow-Origin' header in the response must not be the
    // wildcard '*' when the request's credentials mode is 'include'.
    configuration.setAllowCredentials(false);
    // setAllowedHeaders is important! Without it, OPTIONS preflight request
    // will fail with 403 Invalid CORS request
    configuration.setAllowedHeaders(asList("Authorization", "Cache-Control", "Content-Type"));
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
