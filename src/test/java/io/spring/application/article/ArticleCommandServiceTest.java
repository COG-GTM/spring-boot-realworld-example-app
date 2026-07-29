package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @Mock private ArticleQueryService articleQueryService;

  @Captor private ArgumentCaptor<Article> articleCaptor;

  private ArticleCommandService articleCommandService;

  private User creator;

  private AutoCloseable mocks;

  private AnnotationConfigApplicationContext validationContext;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    articleCommandService = new ArticleCommandService(articleRepository);
    creator = new User("john@test.com", "john", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (validationContext != null) {
      validationContext.close();
    }
    mocks.close();
  }

  @Test
  public void should_create_article_with_slug_generated_from_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to train your Dragon")
            .description("a description")
            .body("a body")
            .tagList(Arrays.asList("java"))
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(created.getSlug()).isEqualTo("how-to-train-your-dragon");
    assertThat(created.getTitle()).isEqualTo("How to train your Dragon");
    assertThat(created.getDescription()).isEqualTo("a description");
    assertThat(created.getBody()).isEqualTo("a body");
    assertThat(created.getUserId()).isEqualTo(creator.getId());
    assertThat(created.getId()).isNotBlank();
    assertThat(created.getCreatedAt()).isNotNull();
    assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
  }

  @Test
  public void should_squash_punctuation_and_repeated_separators_into_the_slug() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Cats, Dogs & Other  Pets?")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(created.getSlug()).isEqualTo("cats-dogs-other-pets-");
  }

  @Test
  public void should_persist_the_created_article_through_the_repository() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("spring"))
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    verify(articleRepository).save(articleCaptor.capture());
    Article saved = articleCaptor.getValue();
    assertThat(saved).isSameAs(created);
    assertThat(saved.getSlug()).isEqualTo("a-new-title");
    assertThat(tagNamesOf(saved)).containsExactly("spring");
  }

  @Test
  public void should_create_one_tag_per_distinct_tag_name() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("tagged")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring", "java"))
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(created.getTags()).hasSize(2);
    assertThat(tagNamesOf(created)).containsExactlyInAnyOrder("java", "spring");
    Set<String> tagIds = created.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
    assertThat(tagIds).hasSize(2);
  }

  @Test
  public void should_create_article_without_any_tag() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("no tags here")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(created.getTags()).isEmpty();
    verify(articleRepository).save(created);
  }

  @Test
  public void should_update_only_the_title_and_regenerate_the_slug() {
    Article article = existingArticle();
    String originalDescription = article.getDescription();
    String originalBody = article.getBody();

    Article updated =
        articleCommandService.updateArticle(article, new UpdateArticleParam("new title", "", ""));

    assertThat(updated).isSameAs(article);
    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getSlug()).isEqualTo("new-title");
    assertThat(updated.getDescription()).isEqualTo(originalDescription);
    assertThat(updated.getBody()).isEqualTo(originalBody);
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_only_the_body_and_keep_the_slug() {
    Article article = existingArticle();
    String originalSlug = article.getSlug();

    Article updated =
        articleCommandService.updateArticle(article, new UpdateArticleParam("", "new body", ""));

    assertThat(updated.getBody()).isEqualTo("new body");
    assertThat(updated.getTitle()).isEqualTo("original title");
    assertThat(updated.getSlug()).isEqualTo(originalSlug);
    assertThat(updated.getDescription()).isEqualTo("original description");
  }

  @Test
  public void should_update_only_the_description_and_keep_the_slug() {
    Article article = existingArticle();
    String originalSlug = article.getSlug();

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("", "", "new description"));

    assertThat(updated.getDescription()).isEqualTo("new description");
    assertThat(updated.getTitle()).isEqualTo("original title");
    assertThat(updated.getBody()).isEqualTo("original body");
    assertThat(updated.getSlug()).isEqualTo(originalSlug);
  }

  @Test
  public void should_update_title_body_and_description_together() {
    Article article = existingArticle();

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("brand new title", "brand new body", "brand new desc"));

    assertThat(updated.getTitle()).isEqualTo("brand new title");
    assertThat(updated.getBody()).isEqualTo("brand new body");
    assertThat(updated.getDescription()).isEqualTo("brand new desc");
    assertThat(updated.getSlug()).isEqualTo("brand-new-title");
    verify(articleRepository).save(articleCaptor.capture());
    assertThat(articleCaptor.getValue().getSlug()).isEqualTo("brand-new-title");
  }

  @Test
  public void should_leave_article_untouched_when_all_update_fields_are_empty() {
    Article article = existingArticle();
    String originalSlug = article.getSlug();

    Article updated =
        articleCommandService.updateArticle(article, new UpdateArticleParam("", "", ""));

    assertThat(updated.getTitle()).isEqualTo("original title");
    assertThat(updated.getBody()).isEqualTo("original body");
    assertThat(updated.getDescription()).isEqualTo("original description");
    assertThat(updated.getSlug()).isEqualTo(originalSlug);
    verify(articleRepository).save(article);
  }

  @Test
  public void should_not_touch_the_repository_before_a_command_is_issued() {
    verify(articleRepository, never()).save(any());
  }

  /**
   * The service is {@code @Validated}, so the {@code @Valid} parameter is only checked by the
   * Spring proxy. A bare instance performs no validation at all, which is what the unit tests above
   * rely on; the constraints themselves are asserted directly against a validator below.
   */
  @Test
  public void should_not_validate_new_article_param_when_service_is_not_proxied() {
    NewArticleParam invalid =
        NewArticleParam.builder()
            .title("")
            .description("")
            .body("")
            .tagList(Collections.emptyList())
            .build();

    Article created = articleCommandService.createArticle(invalid, creator);

    assertThat(created.getTitle()).isEmpty();
    verify(articleRepository).save(created);
  }

  @Test
  public void should_reject_new_article_param_with_blank_fields() {
    NewArticleParam invalid =
        NewArticleParam.builder()
            .title("")
            .description("")
            .body("")
            .tagList(Collections.emptyList())
            .build();

    Set<ConstraintViolation<NewArticleParam>> violations = validator().validate(invalid);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("title", "description", "body");
    assertThat(violations)
        .allSatisfy(violation -> assertThat(violation.getMessage()).isEqualTo("can't be empty"));
  }

  @Test
  public void should_reject_new_article_param_with_a_duplicated_title() {
    when(articleQueryService.findBySlug(eq("duplicated-title"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));
    NewArticleParam duplicated =
        NewArticleParam.builder()
            .title("duplicated title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Set<ConstraintViolation<NewArticleParam>> violations = validator().validate(duplicated);

    assertThat(violations).hasSize(1);
    ConstraintViolation<NewArticleParam> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
    assertThat(violation.getMessage()).isEqualTo("article name exists");
  }

  @Test
  public void should_accept_a_valid_new_article_param() {
    NewArticleParam valid =
        NewArticleParam.builder()
            .title("a fresh title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    assertThat(validator().validate(valid)).isEmpty();
  }

  private Article existingArticle() {
    return new Article(
        "original title",
        "original description",
        "original body",
        Arrays.asList("java"),
        creator.getId());
  }

  private List<String> tagNamesOf(Article article) {
    return article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
  }

  /**
   * Builds a validator whose constraint validators are created by Spring, so that {@link
   * DuplicatedArticleConstraint} gets its {@link ArticleQueryService} injected from the mock above.
   */
  private Validator validator() {
    validationContext = new AnnotationConfigApplicationContext();
    validationContext
        .getBeanFactory()
        .registerSingleton("articleQueryService", articleQueryService);
    validationContext.refresh();
    LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
    validatorFactoryBean.setApplicationContext(validationContext);
    validatorFactoryBean.afterPropertiesSet();
    return validatorFactoryBean;
  }
}
