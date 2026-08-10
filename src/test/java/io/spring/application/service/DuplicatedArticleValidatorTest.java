package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.lang.reflect.Constructor;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DuplicatedArticleValidatorTest {

  private final ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
  private ConstraintValidator<?, String> validator;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    Class<?> validatorClass =
        Class.forName("io.spring.application.article.DuplicatedArticleValidator");
    Constructor<?> constructor = validatorClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    validator = (ConstraintValidator<?, String>) constructor.newInstance();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
  }

  private boolean isValid(String value) {
    return ((ConstraintValidator) validator).isValid(value, null);
  }

  @Test
  void should_be_valid_when_no_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.empty());

    assertThat(isValid("How to train your dragon")).isTrue();
  }

  @Test
  void should_be_invalid_when_an_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.of(mock(ArticleData.class)));

    assertThat(isValid("How to train your dragon")).isFalse();
  }
}
