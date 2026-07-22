package io.spring.api.security;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

public class WebSecurityConfigTest {

  private WebSecurityConfig config;

  @BeforeEach
  public void setUp() {
    config = new WebSecurityConfig();
  }

  @Test
  public void should_provide_jwt_token_filter_bean() {
    assertThat(config.jwtTokenFilter(), is(notNullValue()));
  }

  @Test
  public void should_provide_bcrypt_password_encoder() {
    PasswordEncoder encoder = config.passwordEncoder();
    assertThat(encoder, instanceOf(BCryptPasswordEncoder.class));
    String encoded = encoder.encode("secret");
    assertThat(encoder.matches("secret", encoded), is(true));
    assertThat(encoder.matches("wrong", encoded), is(false));
  }

  @Test
  public void should_configure_cors_for_all_paths() {
    CorsConfigurationSource source = config.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/articles");
    CorsConfiguration corsConfiguration = source.getCorsConfiguration(request);

    assertThat(corsConfiguration, is(notNullValue()));
    assertThat(corsConfiguration.getAllowedOrigins(), hasItem("*"));
    assertThat(corsConfiguration.getAllowedMethods(), hasItem("GET"));
    assertThat(corsConfiguration.getAllowedMethods(), hasItem("POST"));
    assertThat(corsConfiguration.getAllowCredentials(), is(false));
    assertThat(corsConfiguration.getAllowedHeaders(), hasItem("Authorization"));
  }
}
