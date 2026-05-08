package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

  private static final String TITLE = "a new title";
  private static final String SLUG = "a-new-title";

  @BeforeEach
  public void setUp() {
    validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
  }

  @Test
  public void should_be_valid_when_slug_is_not_taken() {
    when(articleQueryService.findBySlug(eq(SLUG), any())).thenReturn(Optional.empty());

    assertTrue(validator.isValid(TITLE, null));
  }

  @Test
  public void should_be_invalid_when_slug_is_already_taken() {
    when(articleQueryService.findBySlug(eq(SLUG), any()))
        .thenReturn(Optional.of(new ArticleData()));

    assertFalse(validator.isValid(TITLE, null));
  }
}
