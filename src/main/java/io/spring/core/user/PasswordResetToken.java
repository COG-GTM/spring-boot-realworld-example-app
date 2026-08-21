package io.spring.core.user;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PasswordResetToken {
  private String id;
  private String token;
  private String userId;
  private DateTime expiresAt;
  private boolean used;

  public PasswordResetToken(String userId, DateTime expiresAt) {
    this.id = UUID.randomUUID().toString();
    this.token = UUID.randomUUID().toString().replace("-", "");
    this.userId = userId;
    this.expiresAt = expiresAt;
    this.used = false;
  }

  public boolean isValidAt(DateTime time) {
    return !used && expiresAt != null && expiresAt.isAfter(time);
  }

  public void markUsed() {
    this.used = true;
  }
}
