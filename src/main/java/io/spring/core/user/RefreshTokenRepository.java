package io.spring.core.user;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository {
  void save(RefreshToken refreshToken);

  Optional<RefreshToken> findByToken(String token);

  Optional<RefreshToken> findByUserId(String userId);

  void revokeByToken(String token);

  void revokeByUserId(String userId);
}
