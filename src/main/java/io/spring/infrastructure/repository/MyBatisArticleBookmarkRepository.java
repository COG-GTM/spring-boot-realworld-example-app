package io.spring.infrastructure.repository;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.mybatis.mapper.ArticleBookmarkMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisArticleBookmarkRepository implements ArticleBookmarkRepository {
  private ArticleBookmarkMapper mapper;

  @Autowired
  public MyBatisArticleBookmarkRepository(ArticleBookmarkMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(ArticleBookmark bookmark) {
    if (mapper.find(bookmark.getArticleId(), bookmark.getUserId()) == null) {
      mapper.insert(bookmark);
    }
  }

  @Override
  public Optional<ArticleBookmark> find(String articleId, String userId) {
    return Optional.ofNullable(mapper.find(articleId, userId));
  }

  @Override
  public void remove(ArticleBookmark bookmark) {
    mapper.delete(bookmark);
  }

  @Override
  public List<String> findBookmarkedArticleIds(String userId) {
    return mapper.findByUserId(userId, 0, Integer.MAX_VALUE);
  }

  @Override
  public int countByUser(String userId) {
    return mapper.countByUserId(userId);
  }
}
