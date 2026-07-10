package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
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
  public void should_be_valid_when_no_article_with_same_slug_exists() {
    String title = "a brand new title";
    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), isNull()))
        .thenReturn(Optional.empty());

    assertTrue(validator.isValid(title, null));
  }

  @Test
  public void should_be_invalid_when_article_with_same_slug_exists() {
    String title = "an existing title";
    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), isNull()))
        .thenReturn(Optional.of(mock(ArticleData.class)));

    assertFalse(validator.isValid(title, null));
  }
}
