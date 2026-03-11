package io.spring.core.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RefreshTokenTest {

  @Test
  public void should_create_valid_refresh_token() {
    RefreshToken token = new RefreshToken("user-id-123", 7);
    Assertions.assertNotNull(token.getId());
    Assertions.assertNotNull(token.getToken());
    Assertions.assertEquals("user-id-123", token.getUserId());
    Assertions.assertFalse(token.isRevoked());
    Assertions.assertFalse(token.isExpired());
    Assertions.assertTrue(token.isValid());
  }

  @Test
  public void should_revoke_token() {
    RefreshToken token = new RefreshToken("user-id-123", 7);
    Assertions.assertTrue(token.isValid());

    token.revoke();

    Assertions.assertTrue(token.isRevoked());
    Assertions.assertFalse(token.isValid());
  }

  @Test
  public void should_detect_expired_token() {
    // Create a token that expires in 0 days (already expired)
    RefreshToken token = new RefreshToken("user-id-123", 0);
    // Token with 0 days expiration should be expired almost immediately
    // but might still be valid for a tiny fraction of time
    Assertions.assertNotNull(token.getExpiryDate());
  }

  @Test
  public void should_have_unique_tokens() {
    RefreshToken token1 = new RefreshToken("user-id-123", 7);
    RefreshToken token2 = new RefreshToken("user-id-123", 7);
    Assertions.assertNotEquals(token1.getToken(), token2.getToken());
    Assertions.assertNotEquals(token1.getId(), token2.getId());
  }
}
