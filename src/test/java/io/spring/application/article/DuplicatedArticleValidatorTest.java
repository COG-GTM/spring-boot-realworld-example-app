package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  private DuplicatedArticleValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
  }

  @Test
  public void should_be_valid_when_no_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("a-new-title"), isNull())).thenReturn(Optional.empty());

    assertThat(validator.isValid("a new title", null)).isTrue();
  }

  @Test
  public void should_be_invalid_when_article_with_the_same_slug_exists() {
    when(articleQueryService.findBySlug(eq("a-new-title"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));

    assertThat(validator.isValid("a new title", null)).isFalse();
  }
}
