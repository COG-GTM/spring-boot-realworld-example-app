package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SpringBootTest
public class WebSecurityConfigTest {

  @Autowired private WebSecurityConfig webSecurityConfig;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenFilter jwtTokenFilter;

  @Autowired private CorsConfigurationSource corsConfigurationSource;

  @Test
  public void should_create_jwt_token_filter_bean() {
    JwtTokenFilter filter = webSecurityConfig.jwtTokenFilter();

    assertNotNull(filter);
    assertTrue(filter instanceof JwtTokenFilter);
  }

  @Test
  public void should_create_password_encoder_bean() {
    PasswordEncoder encoder = webSecurityConfig.passwordEncoder();

    assertNotNull(encoder);
    assertTrue(encoder instanceof BCryptPasswordEncoder);
  }

  @Test
  public void should_autowire_password_encoder() {
    assertNotNull(passwordEncoder);
    assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
  }

  @Test
  public void should_autowire_jwt_token_filter() {
    assertNotNull(jwtTokenFilter);
  }

  @Test
  public void should_create_cors_configuration_source_bean() {
    CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();

    assertNotNull(source);
    assertTrue(source instanceof UrlBasedCorsConfigurationSource);
  }

  @Test
  public void should_autowire_cors_configuration_source() {
    assertNotNull(corsConfigurationSource);
  }

  @Test
  public void should_password_encoder_encode_password() {
    String rawPassword = "testPassword123";

    String encodedPassword = passwordEncoder.encode(rawPassword);

    assertNotNull(encodedPassword);
    assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
  }

  @Test
  public void should_password_encoder_not_match_wrong_password() {
    String rawPassword = "testPassword123";
    String wrongPassword = "wrongPassword";

    String encodedPassword = passwordEncoder.encode(rawPassword);

    assertTrue(!passwordEncoder.matches(wrongPassword, encodedPassword));
  }

  @Test
  public void should_cors_configuration_allow_all_origins() {
    CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();
    CorsConfiguration config =
        ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

    assertNotNull(config);
    assertTrue(config.getAllowedOrigins().contains("*"));
  }

  @Test
  public void should_cors_configuration_allow_required_methods() {
    CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();
    CorsConfiguration config =
        ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

    assertNotNull(config);
    assertTrue(config.getAllowedMethods().containsAll(
        Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH")));
  }

  @Test
  public void should_cors_configuration_allow_required_headers() {
    CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();
    CorsConfiguration config =
        ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

    assertNotNull(config);
    assertTrue(config.getAllowedHeaders().containsAll(
        Arrays.asList("Authorization", "Cache-Control", "Content-Type")));
  }

  @Test
  public void should_cors_configuration_not_allow_credentials() {
    CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource();
    CorsConfiguration config =
        ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");

    assertNotNull(config);
    assertTrue(!config.getAllowCredentials());
  }
}
