package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService = new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 3600);
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
  public void should_generate_different_tokens_for_different_users() {
    User user1 = new User("user1@email.com", "user1", "123", "", "");
    User user2 = new User("user2@email.com", "user2", "456", "", "");

    String token1 = jwtService.toToken(user1);
    String token2 = jwtService.toToken(user2);

    Assertions.assertNotEquals(token1, token2);
  }

  @Test
  public void should_generate_consistent_subject_for_same_user() {
    User user = new User("email@email.com", "username", "123", "", "");

    String token1 = jwtService.toToken(user);
    String token2 = jwtService.toToken(user);

    Optional<String> subject1 = jwtService.getSubFromToken(token1);
    Optional<String> subject2 = jwtService.getSubFromToken(token2);

    Assertions.assertTrue(subject1.isPresent());
    Assertions.assertTrue(subject2.isPresent());
    Assertions.assertEquals(subject1.get(), subject2.get());
  }

  @Test
  public void should_return_empty_for_null_token() {
    Optional<String> optional = jwtService.getSubFromToken(null);
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_return_empty_for_empty_token() {
    Optional<String> optional = jwtService.getSubFromToken("");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_return_empty_for_malformed_token() {
    Optional<String> optional = jwtService.getSubFromToken("not.a.valid.jwt.token");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_return_empty_for_token_with_wrong_signature() {
    String tokenWithWrongSignature =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6OTk5OTk5OTk5OX0.wrongsignature";
    Optional<String> optional = jwtService.getSubFromToken(tokenWithWrongSignature);
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_handle_user_with_special_characters_in_id() {
    User user = new User("special@email.com", "special-user_123", "password", "bio", "image");
    String token = jwtService.toToken(user);

    Assertions.assertNotNull(token);
    Optional<String> optional = jwtService.getSubFromToken(token);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(user.getId(), optional.get());
  }

  @Test
  public void should_create_service_with_different_session_times() {
    JwtService shortSessionService =
        new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 1);
    JwtService longSessionService =
        new DefaultJwtService("123123123123123123123123123123123123123123123123123123123123", 86400);

    User user = new User("email@email.com", "username", "123", "", "");

    String shortToken = shortSessionService.toToken(user);
    String longToken = longSessionService.toToken(user);

    Assertions.assertNotNull(shortToken);
    Assertions.assertNotNull(longToken);
    Assertions.assertNotEquals(shortToken, longToken);
  }
}
