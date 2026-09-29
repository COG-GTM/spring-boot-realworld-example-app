package io.spring.infrastructure.repository;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleChangedEvent;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.infrastructure.mybatis.mapper.ArticleMapper;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisArticleRepository implements ArticleRepository {
  private ArticleMapper articleMapper;
  private ApplicationEventPublisher eventPublisher;

  public MyBatisArticleRepository(
      ArticleMapper articleMapper, ApplicationEventPublisher eventPublisher) {
    this.articleMapper = articleMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void save(Article article) {
    Set<String> affectedSlugs = new HashSet<>();
    affectedSlugs.add(article.getSlug());
    Article existing = articleMapper.findById(article.getId());
    if (existing == null) {
      createNew(article);
    } else {
      affectedSlugs.add(existing.getSlug());
      articleMapper.update(article);
    }
    eventPublisher.publishEvent(new ArticleChangedEvent(article.getId(), affectedSlugs));
  }

  private void createNew(Article article) {
    for (Tag tag : article.getTags()) {
      Tag targetTag =
          Optional.ofNullable(articleMapper.findTag(tag.getName()))
              .orElseGet(
                  () -> {
                    articleMapper.insertTag(tag);
                    return tag;
                  });
      articleMapper.insertArticleTagRelation(article.getId(), targetTag.getId());
    }
    articleMapper.insert(article);
  }

  @Override
  public Optional<Article> findById(String id) {
    return Optional.ofNullable(articleMapper.findById(id));
  }

  @Override
  public Optional<Article> findBySlug(String slug) {
    return Optional.ofNullable(articleMapper.findBySlug(slug));
  }

  @Override
  public void remove(Article article) {
    articleMapper.delete(article.getId());
    eventPublisher.publishEvent(
        new ArticleChangedEvent(article.getId(), Set.of(article.getSlug())));
  }
}
