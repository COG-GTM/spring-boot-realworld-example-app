package io.spring.infrastructure.repository;

import io.spring.core.user.PasswordResetToken;
import io.spring.core.user.PasswordResetTokenRepository;
import io.spring.infrastructure.mybatis.mapper.PasswordResetTokenMapper;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisPasswordResetTokenRepository implements PasswordResetTokenRepository {
  private final PasswordResetTokenMapper passwordResetTokenMapper;

  @Autowired
  public MyBatisPasswordResetTokenRepository(PasswordResetTokenMapper passwordResetTokenMapper) {
    this.passwordResetTokenMapper = passwordResetTokenMapper;
  }

  @Override
  public void save(PasswordResetToken passwordResetToken) {
    if (passwordResetTokenMapper.findById(passwordResetToken.getId()) == null) {
      passwordResetTokenMapper.insert(passwordResetToken);
    } else {
      passwordResetTokenMapper.update(passwordResetToken);
    }
  }

  @Override
  public Optional<PasswordResetToken> findByToken(String token) {
    return Optional.ofNullable(passwordResetTokenMapper.findByToken(token));
  }
}
