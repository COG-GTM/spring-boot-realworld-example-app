package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Configuration;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NewArticleParamTest {

  private ArticleQueryService articleQueryService;
  private ValidatorFactory validatorFactory;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    Configuration<?> configuration = Validation.byDefaultProvider().configure();
    ConstraintValidatorFactory defaultFactory =
        configuration.getDefaultConstraintValidatorFactory();
    validatorFactory =
        configuration
            .constraintValidatorFactory(
                new ConstraintValidatorFactory() {
                  @Override
                  @SuppressWarnings("unchecked")
                  public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                    if (key.equals(DuplicatedArticleValidator.class)) {
                      DuplicatedArticleValidator instance = new DuplicatedArticleValidator();
                      try {
                        Field field =
                            DuplicatedArticleValidator.class.getDeclaredField(
                                "articleQueryService");
                        field.setAccessible(true);
                        field.set(instance, articleQueryService);
                      } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                      }
                      return (T) instance;
                    }
                    return defaultFactory.getInstance(key);
                  }

                  @Override
                  public void releaseInstance(ConstraintValidator<?, ?> instance) {}
                })
            .buildValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterEach
  public void tearDown() {
    validatorFactory.close();
  }

  private Set<String> violatedProperties(NewArticleParam param) {
    return validator.validate(param).stream()
        .map(v -> v.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  @Test
  public void should_expose_all_fields_through_getters_and_builder() {
    List<String> tags = Arrays.asList("java", "spring");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a title")
            .description("a description")
            .body("a body")
            .tagList(tags)
            .build();

    assertThat(param.getTitle()).isEqualTo("a title");
    assertThat(param.getDescription()).isEqualTo("a description");
    assertThat(param.getBody()).isEqualTo("a body");
    assertThat(param.getTagList()).containsExactly("java", "spring");
    assertThat(NewArticleParam.builder().toString()).isNotNull();
  }

  @Test
  public void should_support_all_args_constructor_and_no_args_constructor() {
    NewArticleParam param = new NewArticleParam("t", "d", "b", Arrays.asList("x"));
    assertThat(param.getTitle()).isEqualTo("t");
    assertThat(param.getDescription()).isEqualTo("d");
    assertThat(param.getBody()).isEqualTo("b");
    assertThat(param.getTagList()).containsExactly("x");

    NewArticleParam empty = new NewArticleParam();
    assertThat(empty.getTitle()).isNull();
  }

  @Test
  public void should_pass_validation_for_a_valid_unique_article() {
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a unique title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    assertThat(validator.validate(param)).isEmpty();
  }

  @Test
  public void should_reject_blank_title_description_and_body() {
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
    NewArticleParam param = NewArticleParam.builder().title("").description("").body("").build();

    assertThat(violatedProperties(param)).contains("title", "description", "body");
  }

  @Test
  public void should_carry_can_not_be_empty_message_for_blank_body() {
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
    NewArticleParam param =
        NewArticleParam.builder().title("ok").description("ok").body("").build();

    Set<ConstraintViolation<NewArticleParam>> violations = validator.validate(param);
    assertThat(violations).hasSize(1);
    ConstraintViolation<NewArticleParam> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString()).isEqualTo("body");
    assertThat(violation.getMessage()).isEqualTo("can't be empty");
  }

  @Test
  public void should_reject_duplicated_title() {
    when(articleQueryService.findBySlug(anyString(), any()))
        .thenReturn(Optional.of(new ArticleData()));
    NewArticleParam param =
        NewArticleParam.builder()
            .title("an existing title")
            .description("desc")
            .body("body")
            .build();

    Set<ConstraintViolation<NewArticleParam>> violations = validator.validate(param);
    assertThat(violations.stream().map(v -> v.getPropertyPath().toString())).contains("title");
    assertThat(violations.stream().map(ConstraintViolation::getMessage))
        .contains("article name exists");
  }
}
