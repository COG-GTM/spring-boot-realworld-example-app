package io.spring.application.article;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Focused test for {@link ArticleCommandService}. A minimal Spring context wires the service behind
 * a {@link MethodValidationPostProcessor} so that {@code @Validated}/{@code @Valid} method-level
 * validation (including the {@code @DuplicatedArticleConstraint}) actually fires, while the
 * repositories/query services it depends on are Mockito mocks.
 */
@SpringBootTest(
    classes = {
      ArticleCommandService.class,
      ArticleCommandServiceTest.ValidationConfig.class
    })
public class ArticleCommandServiceTest {

  @Configuration
  static class ValidationConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor(
        LocalValidatorFactoryBean validator) {
      MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
      processor.setValidator(validator);
      return processor;
    }
  }

  @Autowired private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  private User creator;

  @BeforeEach
  public void setUp() {
    creator = new User("author@test.com", "author", "123", "", "");
    when(articleQueryService.findBySlug(any(), any())).thenReturn(Optional.empty());
  }

  @Test
  public void should_create_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    Assertions.assertEquals("a new title", article.getTitle());
    Assertions.assertEquals(creator.getId(), article.getUserId());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_reject_create_article_with_blank_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> articleCommandService.createArticle(param, creator));
  }

  @Test
  public void should_reject_create_article_with_duplicated_title() {
    when(articleQueryService.findBySlug(eq(Article.toSlug("dup title")), any()))
        .thenReturn(Optional.of(new ArticleData()));
    NewArticleParam param =
        NewArticleParam.builder()
            .title("dup title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> articleCommandService.createArticle(param, creator));
  }

  @Test
  public void should_update_article() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    Assertions.assertEquals("new title", updated.getTitle());
    Assertions.assertEquals("new body", updated.getBody());
    Assertions.assertEquals("new desc", updated.getDescription());
    verify(articleRepository).save(article);
  }
}
