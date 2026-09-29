package io.spring.infrastructure.repository;

import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteChangedEvent;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.infrastructure.mybatis.mapper.ArticleFavoriteMapper;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisArticleFavoriteRepository implements ArticleFavoriteRepository {
  private ArticleFavoriteMapper mapper;
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  public MyBatisArticleFavoriteRepository(
      ArticleFavoriteMapper mapper, ApplicationEventPublisher eventPublisher) {
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void save(ArticleFavorite articleFavorite) {
    if (mapper.find(articleFavorite.getArticleId(), articleFavorite.getUserId()) == null) {
      mapper.insert(articleFavorite);
      publishChanged(articleFavorite);
    }
  }

  @Override
  public Optional<ArticleFavorite> find(String articleId, String userId) {
    return Optional.ofNullable(mapper.find(articleId, userId));
  }

  @Override
  public void remove(ArticleFavorite favorite) {
    mapper.delete(favorite);
    publishChanged(favorite);
  }

  private void publishChanged(ArticleFavorite favorite) {
    eventPublisher.publishEvent(
        new ArticleFavoriteChangedEvent(favorite.getArticleId(), favorite.getUserId()));
  }
}
