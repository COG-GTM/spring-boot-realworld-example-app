package io.spring.application.article;

import io.spring.application.ArticleQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;

@Import({
  ArticleCommandService.class,
  ArticleQueryService.class,
  MyBatisArticleRepository.class,
  MyBatisUserRepository.class,
  ValidationAutoConfiguration.class
})
public class ArticleCommandServiceTest extends DbTestBase {
  @Autowired private ArticleCommandService articleCommandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("author@example.com", "author", "123", "", "");
    userRepository.save(user);
  }

  @Test
  public void should_create_article_and_persist_it() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new article")
            .description("a description")
            .body("article body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article created = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(created.getId());
    Assertions.assertEquals("a-new-article", created.getSlug());
    Assertions.assertEquals(user.getId(), created.getUserId());

    Optional<Article> fetched = articleRepository.findById(created.getId());
    Assertions.assertTrue(fetched.isPresent());
    Article saved = fetched.get();
    Assertions.assertEquals("a new article", saved.getTitle());
    Assertions.assertEquals("a description", saved.getDescription());
    Assertions.assertEquals("article body", saved.getBody());
    Assertions.assertEquals(2, saved.getTags().size());
  }

  @Test
  public void should_reject_article_with_duplicated_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("duplicated title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    articleCommandService.createArticle(param, user);

    NewArticleParam duplicated =
        NewArticleParam.builder()
            .title("duplicated title")
            .description("another desc")
            .body("another body")
            .tagList(Arrays.asList("spring"))
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> articleCommandService.createArticle(duplicated, user));
  }

  @Test
  public void should_update_article_and_persist_changes() {
    Article article =
        new Article(
            "origin title", "origin desc", "origin body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    UpdateArticleParam updateParam =
        new UpdateArticleParam("updated title", "updated body", "updated desc");
    Article updated = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals("updated title", updated.getTitle());
    Assertions.assertEquals("updated-title", updated.getSlug());

    Optional<Article> fetched = articleRepository.findById(article.getId());
    Assertions.assertTrue(fetched.isPresent());
    Article saved = fetched.get();
    Assertions.assertEquals("updated title", saved.getTitle());
    Assertions.assertEquals("updated desc", saved.getDescription());
    Assertions.assertEquals("updated body", saved.getBody());
    Assertions.assertEquals("updated-title", saved.getSlug());
  }
}
