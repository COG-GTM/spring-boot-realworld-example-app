package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
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
    JwtService otherService =
        new DefaultJwtService(
            "3213213213213213213213213213213213213213213213213213213213213213", 3600);
    String token = otherService.toToken(new User("email@email.com", "username", "123", "", ""));
    Assertions.assertFalse(jwtService.getSubFromToken(token).isPresent());
  }

  @Test
  public void should_reject_too_short_secret() {
    Assertions.assertThrows(
        IllegalStateException.class, () -> new DefaultJwtService("too-short-secret", 3600));
  }

  @Test
  public void should_generate_random_key_when_no_secret_configured() {
    User user = new User("email@email.com", "username", "123", "", "");
    JwtService first = new DefaultJwtService("", 3600);
    JwtService second = new DefaultJwtService("", 3600);
    Assertions.assertTrue(first.getSubFromToken(first.toToken(user)).isPresent());
    Assertions.assertFalse(second.getSubFromToken(first.toToken(user)).isPresent());
  }
}
