package io.spring.core.user;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class RefreshToken {
  private String id;
  private String userId;
  private String token;
  private DateTime createdAt;
  private DateTime expiresAt;
  private boolean revoked;

  public RefreshToken(String userId, String token, DateTime expiresAt) {
    this.id = UUID.randomUUID().toString();
    this.userId = userId;
    this.token = token;
    this.createdAt = new DateTime();
    this.expiresAt = expiresAt;
    this.revoked = false;
  }

  public void revoke() {
    this.revoked = true;
  }
}
