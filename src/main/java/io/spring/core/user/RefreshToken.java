package io.spring.core.user;

import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshToken {
  private String id;
  private String userId;
  private String token;
  private Date expiryDate;
  private boolean revoked;
  private Date createdAt;

  public RefreshToken(String userId, int expirationDays) {
    this.id = UUID.randomUUID().toString();
    this.userId = userId;
    this.token = UUID.randomUUID().toString();
    this.expiryDate = new Date(System.currentTimeMillis() + (long) expirationDays * 24 * 60 * 60 * 1000);
    this.revoked = false;
    this.createdAt = new Date();
  }

  public boolean isExpired() {
    return new Date().after(this.expiryDate);
  }

  public boolean isValid() {
    return !revoked && !isExpired();
  }

  public void revoke() {
    this.revoked = true;
  }
}
