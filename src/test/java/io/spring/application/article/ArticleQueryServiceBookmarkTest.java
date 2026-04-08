package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  ArticleQueryService.class,
  MyBatisUserRepository.class,
  MyBatisArticleRepository.class,
  MyBatisArticleFavoriteRepository.class,
  MyBatisArticleBookmarkRepository.class
})
public class ArticleQueryServiceBookmarkTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("bookmark-test@gmail.com", "bookmarkuser", "123", "", "");
    userRepository.save(user);
    article =
        new Article(
            "test", "desc", "body", Arrays.asList("java", "spring"), user.getId(), new DateTime());
    articleRepository.save(article);
  }

  @Test
  public void should_get_article_with_bookmark_false_when_not_bookmarked() {
    Optional<ArticleData> optional = queryService.findById(article.getId(), user);
    Assertions.assertTrue(optional.isPresent());

    ArticleData fetched = optional.get();
    Assertions.assertFalse(fetched.isBookmarked());
  }

  @Test
  public void should_get_article_with_bookmark_true_when_bookmarked() {
    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), user.getId()));

    Optional<ArticleData> optional = queryService.findById(article.getId(), user);
    Assertions.assertTrue(optional.isPresent());

    ArticleData fetched = optional.get();
    Assertions.assertTrue(fetched.isBookmarked());
  }

  @Test
  public void should_get_article_list_with_correct_bookmark_flags() {
    User anotherUser = new User("other-bookmark@test.com", "otherbookmark", "123", "", "");
    userRepository.save(anotherUser);

    Article anotherArticle =
        new Article(
            "another article",
            "desc",
            "body",
            Arrays.asList("test"),
            user.getId(),
            new DateTime().minusHours(1));
    articleRepository.save(anotherArticle);

    articleBookmarkRepository.save(new ArticleBookmark(article.getId(), anotherUser.getId()));

    ArticleDataList recentArticles =
        queryService.findRecentArticles(null, null, null, new Page(), anotherUser);
    Assertions.assertEquals(2, recentArticles.getArticleDatas().size());

    for (ArticleData articleData : recentArticles.getArticleDatas()) {
      if (articleData.getId().equals(article.getId())) {
        Assertions.assertTrue(articleData.isBookmarked());
      } else {
        Assertions.assertFalse(articleData.isBookmarked());
      }
    }
  }

  @Test
  public void should_not_set_bookmark_when_user_is_null() {
    Optional<ArticleData> optional = queryService.findById(article.getId(), null);
    Assertions.assertTrue(optional.isPresent());

    ArticleData fetched = optional.get();
    Assertions.assertFalse(fetched.isBookmarked());
  }
}
