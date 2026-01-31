package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.user.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
  void insert(@Param("refreshToken") RefreshToken refreshToken);

  void update(@Param("refreshToken") RefreshToken refreshToken);

  RefreshToken findById(@Param("id") String id);

  RefreshToken findByToken(@Param("token") String token);

  RefreshToken findByUserId(@Param("userId") String userId);

  void revokeByToken(@Param("token") String token);

  void revokeByUserId(@Param("userId") String userId);
}
