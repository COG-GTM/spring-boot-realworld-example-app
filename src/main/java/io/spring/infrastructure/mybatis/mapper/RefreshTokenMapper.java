package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.user.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
  void insert(@Param("refreshToken") RefreshToken refreshToken);

  RefreshToken findByToken(@Param("token") String token);

  void revokeAllByUserId(@Param("userId") String userId);

  void deleteExpired();
}
