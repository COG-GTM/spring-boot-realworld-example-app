package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private static final String SECRET =
      "1231231231231231231231231231231231231231231231231231231231231231";

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService = new DefaultJwtService(SECRET, 3600);
  }

  @Test
  public void should_generate_and_parse_token() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    Assertions.assertNotNull(token);
    Optional<String> optional = jwtService.getSubFromToken(token);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), user.getId());
  }

  @Test
  public void should_get_null_with_wrong_jwt() {
    Optional<String> optional = jwtService.getSubFromToken("123");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_get_null_with_expired_jwt() {
    String token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaXNlbnNpeSIsImV4cCI6MTUwMjE2MTIwNH0.SJB-U60WzxLYNomqLo4G3v3LzFxJKuVrIud8D8Lz3-mgpo9pN1i7C8ikU_jQPJGm8HsC1CquGMI-rSuM7j6LDA";
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_token_signed_with_another_secret() {
    JwtService other =
        new DefaultJwtService(
            "3213213213213213213213213213213213213213213213213213213213213213", 3600);
    String token = other.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_fail_when_secret_missing() {
    Assertions.assertThrows(IllegalStateException.class, () -> new DefaultJwtService(null, 3600));
    Assertions.assertThrows(IllegalStateException.class, () -> new DefaultJwtService("  ", 3600));
  }

  @Test
  public void should_fail_when_secret_too_short() {
    Assertions.assertThrows(
        IllegalStateException.class,
        () ->
            new DefaultJwtService(
                SECRET.substring(0, DefaultJwtService.MINIMUM_SECRET_BYTES - 1), 3600));
  }

  @Test
  public void should_fail_with_previously_committed_secret() {
    String leaked =
        "nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA";
    Assertions.assertThrows(IllegalStateException.class, () -> new DefaultJwtService(leaked, 3600));
  }

  @Test
  public void should_not_ship_a_signing_secret_in_application_properties() throws Exception {
    Properties properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
      properties.load(in);
    }
    Assertions.assertEquals("${JWT_SECRET}", properties.getProperty("jwt.secret"));
  }
}
