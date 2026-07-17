package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
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
  public void should_be_valid_when_no_article_with_slug_exists() {
    String title = "A Fresh Title";
    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), isNull()))
        .thenReturn(Optional.empty());

    assertThat(validator.isValid(title, null)).isTrue();
  }

  @Test
  public void should_be_invalid_when_article_with_slug_exists() {
    String title = "Existing Title";
    when(articleQueryService.findBySlug(eq(Article.toSlug(title)), isNull()))
        .thenReturn(Optional.of(mock(ArticleData.class)));

    assertThat(validator.isValid(title, null)).isFalse();
  }

  @Test
  public void should_look_up_by_slugified_title() {
    when(articleQueryService.findBySlug(eq("hello-world"), isNull())).thenReturn(Optional.empty());

    assertThat(validator.isValid("Hello World", null)).isTrue();
  }
}
