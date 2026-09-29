package io.spring.infrastructure.mybatis.readservice;

import io.spring.core.user.User;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleFavoritesReadService {
  boolean isUserFavorite(@Param("userId") String userId, @Param("articleId") String articleId);

  Set<String> userFavorites(@Param("ids") List<String> ids, @Param("currentUser") User currentUser);
}
