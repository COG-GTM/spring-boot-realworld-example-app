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
  public void save(RefreshToken refreshToken) {
    if (refreshTokenMapper.findById(refreshToken.getId()) == null) {
      refreshTokenMapper.insert(refreshToken);
    } else {
      refreshTokenMapper.update(refreshToken);
    }
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return Optional.ofNullable(refreshTokenMapper.findByToken(token));
  }

  @Override
  public Optional<RefreshToken> findByUserId(String userId) {
    return Optional.ofNullable(refreshTokenMapper.findByUserId(userId));
  }

  @Override
  public void revokeByToken(String token) {
    refreshTokenMapper.revokeByToken(token);
  }

  @Override
  public void revokeByUserId(String userId) {
    refreshTokenMapper.revokeByUserId(userId);
  }
}
