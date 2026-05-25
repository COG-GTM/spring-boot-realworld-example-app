package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  @InjectMocks private DuplicatedArticleValidator validator;

  @Mock private ConstraintValidatorContext context;

  @Test
  void should_be_valid_when_slug_does_not_exist() {
    when(articleQueryService.findBySlug(any(String.class), isNull())).thenReturn(Optional.empty());

    assertTrue(validator.isValid("New Article Title", context));
  }

  @Test
  void should_be_invalid_when_slug_already_exists() {
    ArticleData existing = mock(ArticleData.class);
    when(articleQueryService.findBySlug(any(String.class), isNull()))
        .thenReturn(Optional.of(existing));

    assertFalse(validator.isValid("Existing Title", context));
  }
}
