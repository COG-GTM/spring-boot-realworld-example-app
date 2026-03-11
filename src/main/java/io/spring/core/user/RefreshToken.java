package io.spring.core.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshToken {
  private String id;
  private String userId;
  private String token;
  private Instant expiryDate;
  private boolean revoked;
  private Instant createdAt;

  public RefreshToken(String userId, int expirationDays) {
    this.id = UUID.randomUUID().toString();
    this.userId = userId;
    this.token = UUID.randomUUID().toString();
    this.expiryDate = Instant.now().plusSeconds((long) expirationDays * 24 * 60 * 60);
    this.revoked = false;
    this.createdAt = Instant.now();
  }

  public boolean isExpired() {
    return Instant.now().isAfter(this.expiryDate);
  }

  public boolean isValid() {
    return !revoked && !isExpired();
  }

  public void revoke() {
    this.revoked = true;
  }
}
