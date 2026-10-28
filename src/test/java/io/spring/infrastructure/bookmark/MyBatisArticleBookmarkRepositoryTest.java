package io.spring.infrastructure.bookmark;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleBookmarkRepository.class})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Test
  public void should_save_and_fetch_article_bookmark() {
    DateTime createdAt = new DateTime(1_600_000_000_123L);
    ArticleBookmark bookmark = new ArticleBookmark("article-1", "user-1", createdAt);

    articleBookmarkRepository.save(bookmark);

    ArticleBookmark fetched =
        articleBookmarkRepository.find("article-1", "user-1").orElseThrow(AssertionError::new);
    Assertions.assertEquals(bookmark, fetched);
    Assertions.assertEquals(createdAt, fetched.getCreatedAt());
  }

  @Test
  public void should_preserve_original_created_at_on_repeated_save() {
    DateTime originalCreatedAt = new DateTime(1_600_000_000_123L);
    ArticleBookmark bookmark = new ArticleBookmark("article-1", "user-1", originalCreatedAt);
    articleBookmarkRepository.save(bookmark);
    articleBookmarkRepository.save(
        new ArticleBookmark("article-1", "user-1", originalCreatedAt.plusHours(1)));

    ArticleBookmark fetched =
        articleBookmarkRepository.find("article-1", "user-1").orElseThrow(AssertionError::new);
    Assertions.assertEquals(originalCreatedAt, fetched.getCreatedAt());
  }

  @Test
  public void should_remove_bookmark_and_ignore_nonexistent_bookmark() {
    ArticleBookmark bookmark = new ArticleBookmark("article-1", "user-1");
    articleBookmarkRepository.save(bookmark);

    articleBookmarkRepository.remove(bookmark);
    articleBookmarkRepository.remove(bookmark);

    Assertions.assertFalse(articleBookmarkRepository.find("article-1", "user-1").isPresent());
  }
}
