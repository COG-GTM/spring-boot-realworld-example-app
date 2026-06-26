package io.spring.infrastructure.bookmark;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleBookmarkRepository.class})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Autowired
  private io.spring.infrastructure.mybatis.mapper.ArticleBookmarkMapper articleBookmarkMapper;

  @Test
  public void should_save_and_fetch_articleBookmark_success() {
    ArticleBookmark articleBookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(articleBookmark);
    Assertions.assertNotNull(
        articleBookmarkMapper.find(articleBookmark.getArticleId(), articleBookmark.getUserId()));
  }

  @Test
  public void should_remove_bookmark_success() {
    ArticleBookmark articleBookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(articleBookmark);
    articleBookmarkRepository.remove(articleBookmark);
    Assertions.assertFalse(articleBookmarkRepository.find("123", "456").isPresent());
  }
}
