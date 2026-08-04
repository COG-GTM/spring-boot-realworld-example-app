package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  @Mock private ConstraintValidatorContext context;

  private DuplicatedArticleValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
  }

  @Test
  public void should_be_valid_when_no_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.empty());

    assertThat(validator.isValid("How to train your dragon", context)).isTrue();
  }

  @Test
  public void should_be_invalid_when_an_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThat(validator.isValid("How to train your dragon", context)).isFalse();
  }

  @Test
  public void should_lookup_by_the_slugified_title() {
    when(articleQueryService.findBySlug(eq("a-b-c"), isNull())).thenReturn(Optional.empty());

    assertThat(validator.isValid("A B C", context)).isTrue();
  }
}
