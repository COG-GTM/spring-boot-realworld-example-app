package io.spring.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceEdgeCaseTest {

  private DefaultJwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService =
        new DefaultJwtService(
            "test-only-signing-key-not-a-real-secret-just-for-unit-tests-padding", 86400);
  }

  @Test
  void should_generate_token_for_user() {
    User user = new User("test@example.com", "testuser", "password", "", "");
    String token = jwtService.toToken(user);
    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void should_extract_subject_from_valid_token() {
    User user = new User("test@example.com", "testuser", "password", "", "");
    String token = jwtService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertTrue(subject.isPresent());
    assertEquals(user.getId(), subject.get());
  }

  @Test
  void should_return_empty_for_invalid_token() {
    Optional<String> subject = jwtService.getSubFromToken("invalid.token.here");
    assertFalse(subject.isPresent());
  }

  @Test
  void should_return_empty_for_empty_token() {
    Optional<String> subject = jwtService.getSubFromToken("");
    assertFalse(subject.isPresent());
  }

  @Test
  void should_return_empty_for_malformed_token() {
    Optional<String> subject = jwtService.getSubFromToken("not-a-jwt");
    assertFalse(subject.isPresent());
  }

  @Test
  void should_return_empty_for_token_with_wrong_signature() {
    DefaultJwtService otherService =
        new DefaultJwtService(
            "another-test-only-key-not-real-just-for-verifying-wrong-signature-pad", 86400);
    User user = new User("test@example.com", "testuser", "password", "", "");
    String token = otherService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertFalse(subject.isPresent());
  }

  @Test
  void should_generate_different_tokens_for_different_users() {
    User user1 = new User("a@example.com", "user1", "pass", "", "");
    User user2 = new User("b@example.com", "user2", "pass", "", "");
    String token1 = jwtService.toToken(user1);
    String token2 = jwtService.toToken(user2);
    assertNotEquals(token1, token2);
  }

  @Test
  void should_return_empty_for_expired_token() {
    DefaultJwtService expiredService =
        new DefaultJwtService(
            "test-only-signing-key-not-a-real-secret-just-for-unit-tests-padding", 0);
    User user = new User("test@example.com", "testuser", "password", "", "");
    String token = expiredService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertFalse(subject.isPresent());
  }
}
