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
import java.util.Collections;
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
    user = new User("test@test.com", "testuser", "123", "", "");
    userRepository.save(user);
  }

  @Test
  public void should_create_article_success() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("test article")
            .description("test description")
            .body("test body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("test article", article.getTitle());
    Assertions.assertEquals("test description", article.getDescription());
    Assertions.assertEquals("test body", article.getBody());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertEquals(2, article.getTags().size());

    Assertions.assertTrue(articleRepository.findById(article.getId()).isPresent());
  }

  @Test
  public void should_create_article_with_empty_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("no tags article")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertNotNull(article.getId());
    Assertions.assertEquals("no tags article", article.getTitle());
    Assertions.assertTrue(article.getTags().isEmpty());
  }

  @Test
  public void should_update_article_title() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("original title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    Article article = articleCommandService.createArticle(createParam, user);

    UpdateArticleParam updateParam = new UpdateArticleParam("updated title", "", "");
    Article updated = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals("updated title", updated.getTitle());
    Assertions.assertEquals("desc", updated.getDescription());
    Assertions.assertEquals("body", updated.getBody());
  }

  @Test
  public void should_update_article_description_and_body() {
    NewArticleParam createParam =
        NewArticleParam.builder()
            .title("title")
            .description("old desc")
            .body("old body")
            .tagList(Arrays.asList("java"))
            .build();
    Article article = articleCommandService.createArticle(createParam, user);

    UpdateArticleParam updateParam = new UpdateArticleParam("", "new body", "new description");
    Article updated = articleCommandService.updateArticle(article, updateParam);

    Assertions.assertEquals("title", updated.getTitle());
    Assertions.assertEquals("new description", updated.getDescription());
    Assertions.assertEquals("new body", updated.getBody());
  }

  @Test
  public void should_throw_validation_error_for_blank_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> articleCommandService.createArticle(param, user));
  }

  @Test
  public void should_throw_validation_error_for_blank_body() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("valid title")
            .description("desc")
            .body("")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> articleCommandService.createArticle(param, user));
  }

  @Test
  public void should_throw_validation_error_for_blank_description() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("another valid title")
            .description("")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> articleCommandService.createArticle(param, user));
  }

  @Test
  public void should_throw_validation_error_for_duplicated_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("duplicate title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    articleCommandService.createArticle(param, user);

    NewArticleParam duplicateParam =
        NewArticleParam.builder()
            .title("duplicate title")
            .description("another desc")
            .body("another body")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> articleCommandService.createArticle(duplicateParam, user));
  }
}
