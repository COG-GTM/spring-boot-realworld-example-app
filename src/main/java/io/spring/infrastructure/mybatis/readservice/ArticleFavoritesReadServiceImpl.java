package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.ArticleFavoriteCount;
import io.spring.core.article.ArticleFavoritesReadService;
import io.spring.core.user.User;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ArticleFavoritesReadServiceImpl implements ArticleFavoritesReadService {
  private final MyBatisArticleFavoritesReadServiceMapper mapper;

  public ArticleFavoritesReadServiceImpl(MyBatisArticleFavoritesReadServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean isUserFavorite(String userId, String articleId) {
    return mapper.isUserFavorite(userId, articleId);
  }

  @Override
  public int articleFavoriteCount(String articleId) {
    return mapper.articleFavoriteCount(articleId);
  }

  @Override
  public List<ArticleFavoriteCount> articlesFavoriteCount(List<String> ids) {
    return mapper.articlesFavoriteCount(ids);
  }

  @Override
  public Set<String> userFavorites(List<String> ids, User currentUser) {
    return mapper.userFavorites(ids, currentUser);
  }
}
