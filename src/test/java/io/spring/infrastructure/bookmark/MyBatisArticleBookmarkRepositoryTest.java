package io.spring.infrastructure.bookmark;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({MyBatisArticleBookmarkRepository.class})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Test
  public void should_save_and_fetch_bookmark_success() {
    ArticleBookmark bookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(bookmark);
    Assertions.assertTrue(articleBookmarkRepository.find("123", "456").isPresent());
  }

  @Test
  public void should_remove_bookmark_success() {
    ArticleBookmark bookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(bookmark);
    articleBookmarkRepository.remove(bookmark);
    Assertions.assertFalse(articleBookmarkRepository.find("123", "456").isPresent());
  }

  @Test
  public void should_list_and_count_bookmarked_article_ids() {
    articleBookmarkRepository.save(new ArticleBookmark("article1", "user1"));
    articleBookmarkRepository.save(new ArticleBookmark("article2", "user1"));
    articleBookmarkRepository.save(new ArticleBookmark("article3", "user2"));

    List<String> ids = articleBookmarkRepository.findBookmarkedArticleIds("user1");
    Assertions.assertEquals(2, ids.size());
    Assertions.assertTrue(ids.contains("article1"));
    Assertions.assertTrue(ids.contains("article2"));
    Assertions.assertEquals(2, articleBookmarkRepository.countByUser("user1"));
    Assertions.assertEquals(1, articleBookmarkRepository.countByUser("user2"));
  }
}
