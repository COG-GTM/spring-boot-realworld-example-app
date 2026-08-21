package io.spring.core.user;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository {
  void save(PasswordResetToken passwordResetToken);

  Optional<PasswordResetToken> findByToken(String token);
}
