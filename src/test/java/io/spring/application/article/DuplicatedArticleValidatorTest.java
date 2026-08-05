package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  @Mock private ConstraintValidatorContext context;

  @InjectMocks private DuplicatedArticleValidator validator;

  @Test
  public void should_be_valid_when_no_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("a-new-title"), isNull())).thenReturn(Optional.empty());

    assertThat(validator.isValid("a new title", context)).isTrue();
  }

  @Test
  public void should_be_invalid_when_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("a-new-title"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThat(validator.isValid("a new title", context)).isFalse();
  }

  @Test
  public void should_lookup_by_slugified_title() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThat(validator.isValid("How To Train Your Dragon", context)).isFalse();
  }
}
