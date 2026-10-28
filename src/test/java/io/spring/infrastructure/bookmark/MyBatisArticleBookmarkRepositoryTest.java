package io.spring.infrastructure.bookmark;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleBookmarkRepository.class})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Test
  public void should_save_and_fetch_articleBookmark_success() {
    DateTime createdAt = new DateTime().minusMinutes(5);
    articleBookmarkRepository.save(new ArticleBookmark("123", "456", createdAt));

    Optional<ArticleBookmark> found = articleBookmarkRepository.find("123", "456");
    Assertions.assertTrue(found.isPresent());
    Assertions.assertEquals("123", found.get().getArticleId());
    Assertions.assertEquals("456", found.get().getUserId());
    Assertions.assertEquals(createdAt.getMillis(), found.get().getCreatedAt().getMillis());
    Assertions.assertFalse(articleBookmarkRepository.find("123", "other").isPresent());
  }

  @Test
  public void should_save_be_idempotent() {
    DateTime firstSavedAt = new DateTime().minusMinutes(5);
    articleBookmarkRepository.save(new ArticleBookmark("123", "456", firstSavedAt));
    Assertions.assertDoesNotThrow(
        () -> articleBookmarkRepository.save(new ArticleBookmark("123", "456")));

    Optional<ArticleBookmark> found = articleBookmarkRepository.find("123", "456");
    Assertions.assertTrue(found.isPresent());
    Assertions.assertEquals(firstSavedAt.getMillis(), found.get().getCreatedAt().getMillis());
  }

  @Test
  public void should_remove_bookmark_success() {
    ArticleBookmark articleBookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(articleBookmark);
    articleBookmarkRepository.remove(articleBookmark);
    Assertions.assertFalse(articleBookmarkRepository.find("123", "456").isPresent());
    Assertions.assertDoesNotThrow(() -> articleBookmarkRepository.remove(articleBookmark));
  }
}
