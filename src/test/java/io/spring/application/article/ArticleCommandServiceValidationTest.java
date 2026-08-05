package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Drives {@link ArticleCommandService} through a real bean validation setup so the constraint
 * annotations on {@link NewArticleParam} (including {@link DuplicatedArticleConstraint}) are
 * actually exercised.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ArticleCommandServiceValidationTest.TestConfiguration.class)
public class ArticleCommandServiceValidationTest {

  @Autowired private ArticleCommandService articleCommandService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleQueryService articleQueryService;

  private User creator;

  @BeforeEach
  public void setUp() {
    reset(articleRepository, articleQueryService);
    creator = new User("aisensiy@gmail.com", "aisensiy", "123", "", "");
  }

  @Test
  public void should_create_article_when_param_is_valid() {
    when(articleQueryService.findBySlug(anyString(), isNull())).thenReturn(Optional.empty());

    Article article = articleCommandService.createArticle(validParam("a new title"), creator);

    assertThat(article.getSlug()).isEqualTo("a-new-title");
    verify(articleRepository).save(article);
  }

  @Test
  public void should_reject_blank_title() {
    when(articleQueryService.findBySlug(anyString(), isNull())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> articleCommandService.createArticle(validParam(""), creator))
        .satisfies(e -> assertThat(messagesOf(e)).contains("can't be empty"));

    verify(articleRepository, never()).save(any(Article.class));
  }

  @Test
  public void should_reject_duplicated_title() {
    when(articleQueryService.findBySlug(anyString(), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> articleCommandService.createArticle(validParam("a new title"), creator))
        .satisfies(e -> assertThat(messagesOf(e)).contains("article name exists"));

    verify(articleRepository, never()).save(any(Article.class));
  }

  @Test
  public void should_reject_blank_description_and_body() {
    when(articleQueryService.findBySlug(anyString(), isNull())).thenReturn(Optional.empty());
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("")
            .body("")
            .tagList(Arrays.asList("java"))
            .build();

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> articleCommandService.createArticle(param, creator))
        .satisfies(e -> assertThat(e.getConstraintViolations()).hasSize(2));
  }

  @Test
  public void should_update_article_without_validation_error() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());

    articleCommandService.updateArticle(article, new UpdateArticleParam("new title", "", ""));

    assertThat(article.getSlug()).isEqualTo("new-title");
    verify(articleRepository).save(article);
  }

  private NewArticleParam validParam(String title) {
    return NewArticleParam.builder()
        .title(title)
        .description("desc")
        .body("body")
        .tagList(Arrays.asList("java"))
        .build();
  }

  private Set<String> messagesOf(ConstraintViolationException exception) {
    return exception.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    public ArticleRepository articleRepository() {
      return mock(ArticleRepository.class);
    }

    @Bean
    public ArticleQueryService articleQueryService() {
      return mock(ArticleQueryService.class);
    }

    @Bean
    public ArticleCommandService articleCommandService(ArticleRepository articleRepository) {
      return new ArticleCommandService(articleRepository);
    }

    @Bean
    public static LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(
        LocalValidatorFactoryBean validator) {
      MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
      processor.setValidator(validator);
      return processor;
    }
  }
}
