package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

public class WebSecurityConfigTest {

  @Test
  void should_create_jwt_token_filter() {
    WebSecurityConfig config = new WebSecurityConfig();
    JwtTokenFilter filter = config.jwtTokenFilter();
    assertNotNull(filter);
  }

  @Test
  void should_create_password_encoder() {
    WebSecurityConfig config = new WebSecurityConfig();
    PasswordEncoder encoder = config.passwordEncoder();
    assertNotNull(encoder);
    String encoded = encoder.encode("password");
    assertTrue(encoder.matches("password", encoded));
  }

  @Test
  void should_create_cors_configuration() {
    WebSecurityConfig config = new WebSecurityConfig();
    CorsConfigurationSource source = config.corsConfigurationSource();
    assertNotNull(source);
  }
}
