package io.spring.infrastructure.service;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceTest {

  private JwtService jwtService;
  private static final String SECRET = "123123123123123123123123123123123123123123123123123123123123";

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
  public void should_generate_token_with_three_parts() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    Assertions.assertEquals(3, parts.length, "JWT should have 3 parts: header.payload.signature");
  }

  @Test
  public void should_generate_token_with_valid_header() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    String header = new String(Base64.getUrlDecoder().decode(parts[0]));
    Assertions.assertTrue(header.contains("\"alg\""), "Header should contain algorithm");
    Assertions.assertTrue(header.contains("HS"), "Algorithm should be HMAC-based");
  }

  @Test
  public void should_generate_token_with_subject_in_payload() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
    Assertions.assertTrue(payload.contains("\"sub\""), "Payload should contain subject claim");
    Assertions.assertTrue(payload.contains(user.getId()), "Subject should be user ID");
  }

  @Test
  public void should_generate_token_with_expiration_in_payload() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
    Assertions.assertTrue(payload.contains("\"exp\""), "Payload should contain expiration claim");
  }

  @Test
  public void should_reject_token_with_invalid_signature() {
    User user = new User("email@email.com", "username", "123", "", "");
    String token = jwtService.toToken(user);
    String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
    Optional<String> optional = jwtService.getSubFromToken(tamperedToken);
    Assertions.assertFalse(optional.isPresent(), "Tampered token should be rejected");
  }

  @Test
  public void should_reject_token_signed_with_different_key() {
    JwtService otherService = new DefaultJwtService("different_secret_key_that_is_long_enough_for_hmac_sha", 3600);
    User user = new User("email@email.com", "username", "123", "", "");
    String tokenFromOtherService = otherService.toToken(user);
    Optional<String> optional = jwtService.getSubFromToken(tokenFromOtherService);
    Assertions.assertFalse(optional.isPresent(), "Token signed with different key should be rejected");
  }

  @Test
  public void should_generate_different_tokens_for_different_users() {
    User user1 = new User("email1@email.com", "username1", "123", "", "");
    User user2 = new User("email2@email.com", "username2", "456", "", "");
    String token1 = jwtService.toToken(user1);
    String token2 = jwtService.toToken(user2);
    Assertions.assertNotEquals(token1, token2, "Different users should have different tokens");
  }

  @Test
  public void should_handle_empty_token() {
    Optional<String> optional = jwtService.getSubFromToken("");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_handle_null_token() {
    Optional<String> optional = jwtService.getSubFromToken(null);
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_handle_malformed_token() {
    Optional<String> optional = jwtService.getSubFromToken("not.a.valid.jwt.token");
    Assertions.assertFalse(optional.isPresent());
  }
}
