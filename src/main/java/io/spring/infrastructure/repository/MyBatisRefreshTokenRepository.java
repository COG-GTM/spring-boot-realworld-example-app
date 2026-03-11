package io.spring.infrastructure.repository;

import io.spring.core.user.RefreshToken;
import io.spring.core.user.RefreshTokenRepository;
import io.spring.infrastructure.mybatis.mapper.RefreshTokenMapper;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {
  private final RefreshTokenMapper refreshTokenMapper;

  @Autowired
  public MyBatisRefreshTokenRepository(RefreshTokenMapper refreshTokenMapper) {
    this.refreshTokenMapper = refreshTokenMapper;
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return Optional.ofNullable(refreshTokenMapper.findByToken(token));
  }

  @Override
  public void save(RefreshToken refreshToken) {
    refreshTokenMapper.insert(refreshToken);
  }

  @Override
  public void revokeAllByUserId(String userId) {
    refreshTokenMapper.revokeAllByUserId(userId);
  }

  @Override
  public void deleteExpired() {
    refreshTokenMapper.deleteExpired();
  }
}
