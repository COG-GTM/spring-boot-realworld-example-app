package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {
  @Mock private ArticleQueryService articleQueryService;

  @InjectMocks private DuplicatedArticleValidator validator;

  @Test
  public void should_be_valid_when_no_article_with_same_slug() {
    when(articleQueryService.findBySlug(eq("brand-new-title"), isNull()))
        .thenReturn(Optional.empty());

    assertTrue(validator.isValid("Brand New Title", null));
  }

  @Test
  public void should_be_invalid_when_article_with_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("existing-title"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertFalse(validator.isValid("Existing Title", null));
  }

  @Test
  public void should_look_up_by_slugified_title() {
    when(articleQueryService.findBySlug(eq("how-to-train-your-dragon"), isNull()))
        .thenReturn(Optional.empty());

    validator.isValid("How To Train Your Dragon", null);

    verify(articleQueryService).findBySlug(eq("how-to-train-your-dragon"), isNull());
  }
}
