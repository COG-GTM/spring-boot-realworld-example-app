package io.spring.infrastructure.bookmark;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  MyBatisArticleBookmarkRepository.class,
  MyBatisArticleRepository.class,
  MyBatisUserRepository.class
})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;
  private Article article1;
  private Article article2;

  @BeforeEach
  public void setUp() {
    user = new User("john@example.com", "john", "123", "", "");
    userRepository.save(user);
    article1 =
        new Article("title1", "desc", "body", Collections.singletonList("java"), user.getId());
    article2 =
        new Article("title2", "desc", "body", Collections.singletonList("java"), user.getId());
    articleRepository.save(article1);
    articleRepository.save(article2);
  }

  @Test
  public void should_save_and_fetch_bookmark_success() {
    ArticleBookmark bookmark = new ArticleBookmark(article1.getId(), user.getId());
    articleBookmarkRepository.save(bookmark);
    Assertions.assertTrue(
        articleBookmarkRepository.find(article1.getId(), user.getId()).isPresent());
  }

  @Test
  public void should_not_duplicate_bookmark_on_repeated_save() {
    ArticleBookmark bookmark = new ArticleBookmark(article1.getId(), user.getId());
    articleBookmarkRepository.save(bookmark);
    articleBookmarkRepository.save(bookmark);
    Assertions.assertEquals(1, articleBookmarkRepository.countByUser(user.getId()));
  }

  @Test
  public void should_remove_bookmark_success() {
    ArticleBookmark bookmark = new ArticleBookmark(article1.getId(), user.getId());
    articleBookmarkRepository.save(bookmark);
    articleBookmarkRepository.remove(bookmark);
    Assertions.assertFalse(
        articleBookmarkRepository.find(article1.getId(), user.getId()).isPresent());
  }

  @Test
  public void should_list_and_count_bookmarked_article_ids() {
    articleBookmarkRepository.save(new ArticleBookmark(article1.getId(), user.getId()));
    articleBookmarkRepository.save(new ArticleBookmark(article2.getId(), user.getId()));

    List<String> ids = articleBookmarkRepository.findBookmarkedArticleIds(user.getId());
    Assertions.assertEquals(2, ids.size());
    Assertions.assertTrue(ids.contains(article1.getId()));
    Assertions.assertTrue(ids.contains(article2.getId()));
    Assertions.assertEquals(2, articleBookmarkRepository.countByUser(user.getId()));
  }

  @Test
  public void should_exclude_bookmarks_pointing_to_deleted_articles() {
    articleBookmarkRepository.save(new ArticleBookmark(article1.getId(), user.getId()));
    articleBookmarkRepository.save(new ArticleBookmark(article2.getId(), user.getId()));

    articleRepository.remove(article2);

    List<String> ids = articleBookmarkRepository.findBookmarkedArticleIds(user.getId());
    Assertions.assertEquals(Collections.singletonList(article1.getId()), ids);
    Assertions.assertEquals(1, articleBookmarkRepository.countByUser(user.getId()));
  }
}
