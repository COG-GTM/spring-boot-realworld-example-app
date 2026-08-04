package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

public class ArticleCommandServiceTest {

  private ArticleRepository articleRepository;
  private ArticleQueryService articleQueryService;
  private AnnotationConfigApplicationContext context;
  private ArticleCommandService articleCommandService;
  private User user;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleQueryService = mock(ArticleQueryService.class);
    user = new User("john@example.com", "john", "123", "", "");

    // ArticleCommandService is @Validated: method level bean validation only happens through the
    // Spring proxy, and DuplicatedArticleValidator gets its ArticleQueryService injected by the
    // Spring backed constraint validator factory.
    context = new AnnotationConfigApplicationContext();
    context.registerBean(ArticleRepository.class, () -> articleRepository);
    context.registerBean(ArticleQueryService.class, () -> articleQueryService);
    context.registerBean(LocalValidatorFactoryBean.class, LocalValidatorFactoryBean::new);
    context.registerBean(
        MethodValidationPostProcessor.class,
        () -> {
          MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
          processor.setValidator(context.getBean(LocalValidatorFactoryBean.class));
          return processor;
        });
    context.registerBean(ArticleCommandService.class);
    context.refresh();

    articleCommandService = context.getBean(ArticleCommandService.class);
  }

  @AfterEach
  public void tearDown() {
    context.close();
  }

  @Test
  public void should_create_and_save_article_from_the_param() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to train your dragon")
            .description("Ever wonder how?")
            .body("It takes a Jacobian")
            .tagList(Arrays.asList("dragons", "training"))
            .build();
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.empty());

    Article created = articleCommandService.createArticle(param, user);

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(captor.capture());
    Article saved = captor.getValue();

    assertThat(saved).isSameAs(created);
    assertThat(saved.getTitle()).isEqualTo("How to train your dragon");
    assertThat(saved.getSlug()).isEqualTo("how-to-train-your-dragon");
    assertThat(saved.getDescription()).isEqualTo("Ever wonder how?");
    assertThat(saved.getBody()).isEqualTo("It takes a Jacobian");
    assertThat(saved.getUserId()).isEqualTo(user.getId());
    assertThat(saved.getTags())
        .extracting(Tag::getName)
        .containsExactlyInAnyOrder("dragons", "training");
    assertThat(saved.getId()).isNotBlank();
  }

  @Test
  public void should_deduplicate_the_tag_list_of_a_new_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Tagged article")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();
    when(articleQueryService.findBySlug(eq("tagged-article"), isNull()))
        .thenReturn(Optional.empty());

    Article created = articleCommandService.createArticle(param, user);

    assertThat(created.getTags())
        .extracting(Tag::getName)
        .containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_reject_creation_when_the_title_is_already_used() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to train your dragon")
            .description("Ever wonder how?")
            .body("It takes a Jacobian")
            .tagList(Arrays.asList("dragons"))
            .build();
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> articleCommandService.createArticle(param, user))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("article name exists"));

    verify(articleRepository, never()).save(any());
  }

  @Test
  public void should_reject_creation_when_required_fields_are_blank() {
    NewArticleParam param =
        NewArticleParam.builder().title("").description("").body("").tagList(List.of()).build();

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> articleCommandService.createArticle(param, user))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .contains("can't be empty"));

    verify(articleRepository, never()).save(any());
  }

  @Test
  public void should_update_title_slug_description_and_body() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), user.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new desc"));

    verify(articleRepository).save(article);
    assertThat(updated).isSameAs(article);
    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getSlug()).isEqualTo("new-title");
    assertThat(updated.getDescription()).isEqualTo("new desc");
    assertThat(updated.getBody()).isEqualTo("new body");
  }

  @Test
  public void should_keep_the_fields_left_empty_in_the_update_param() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), user.getId());

    Article updated =
        articleCommandService.updateArticle(article, new UpdateArticleParam("", "", "new desc"));

    verify(articleRepository).save(article);
    assertThat(updated.getTitle()).isEqualTo("old title");
    assertThat(updated.getSlug()).isEqualTo("old-title");
    assertThat(updated.getBody()).isEqualTo("old body");
    assertThat(updated.getDescription()).isEqualTo("new desc");
  }
}
