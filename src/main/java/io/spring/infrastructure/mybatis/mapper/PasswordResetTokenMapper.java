package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.user.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PasswordResetTokenMapper {
  void insert(@Param("passwordResetToken") PasswordResetToken passwordResetToken);

  void update(@Param("passwordResetToken") PasswordResetToken passwordResetToken);

  PasswordResetToken findById(@Param("id") String id);

  PasswordResetToken findByToken(@Param("token") String token);
}
