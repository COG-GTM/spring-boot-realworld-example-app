package io.spring.api.security;

import static java.util.Arrays.asList;

import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, UserRepository userRepository, JwtService jwtService) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(ant(HttpMethod.OPTIONS, "/**"))
                    .permitAll()
                    .requestMatchers(ant("/graphiql"), ant("/graphql"))
                    .permitAll()
                    .requestMatchers(ant(HttpMethod.GET, "/articles/feed"))
                    .authenticated()
                    .requestMatchers(
                        ant(HttpMethod.POST, "/users"), ant(HttpMethod.POST, "/users/login"))
                    .permitAll()
                    .requestMatchers(
                        ant(HttpMethod.GET, "/articles/**"),
                        ant(HttpMethod.GET, "/profiles/**"),
                        ant(HttpMethod.GET, "/tags"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new JwtTokenFilter(userRepository, jwtService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private static AntPathRequestMatcher ant(String pattern) {
    return new AntPathRequestMatcher(pattern);
  }

  private static AntPathRequestMatcher ant(HttpMethod method, String pattern) {
    return new AntPathRequestMatcher(pattern, method.name());
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
