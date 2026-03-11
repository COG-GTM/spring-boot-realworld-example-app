package io.spring.core.user;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository {
  Optional<RefreshToken> findByToken(String token);

  void save(RefreshToken refreshToken);

  void revokeAllByUserId(String userId);

  void deleteExpired();
}
