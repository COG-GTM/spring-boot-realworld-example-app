package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DuplicatedArticleValidatorTest {

  @Test
  public void should_reject_existing_article_title() {
    ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
    when(articleQueryService.findBySlug(eq("a-title"), eq(null)))
        .thenReturn(Optional.of(mock(ArticleData.class)));
    DuplicatedArticleValidator validator = validatorWith(articleQueryService);

    assertThat(validator.isValid("A Title", null), is(false));
  }

  @Test
  public void should_accept_new_article_title() {
    ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
    when(articleQueryService.findBySlug(eq("a-title"), eq(null))).thenReturn(Optional.empty());
    DuplicatedArticleValidator validator = validatorWith(articleQueryService);

    assertThat(validator.isValid("A Title", null), is(true));
  }

  private DuplicatedArticleValidator validatorWith(ArticleQueryService articleQueryService) {
    DuplicatedArticleValidator validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
    return validator;
  }
}
