package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.user.User;
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
  void setUp() {
    validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
  }

  @Test
  void should_be_valid_when_no_article_has_the_slug() {
    when(articleQueryService.findBySlug("new-title", null)).thenReturn(Optional.empty());

    assertThat(validator.isValid("new title", context)).isTrue();
  }

  @Test
  void should_be_invalid_when_an_article_already_has_the_slug() {
    User user = TestHelper.userFixture("john");
    ArticleData existing = TestHelper.articleDataFixture("john", user);
    when(articleQueryService.findBySlug("existing-title", null)).thenReturn(Optional.of(existing));

    assertThat(validator.isValid("Existing Title", context)).isFalse();
  }

  @Test
  void should_look_up_the_slugified_title() {
    when(articleQueryService.findBySlug("a-fancy-title", null)).thenReturn(Optional.empty());

    assertThat(validator.isValid("A Fancy, Title", context)).isTrue();
  }

  @Test
  void should_be_valid_when_title_is_empty() {
    when(articleQueryService.findBySlug("", null)).thenReturn(Optional.empty());

    assertThat(validator.isValid("", context)).isTrue();
  }

  /**
   * Documents current behavior: the validator slugifies the value before any null check, so a null
   * title blows up instead of being left to {@code @NotBlank}.
   */
  @Test
  void should_throw_when_title_is_null() {
    assertThatThrownBy(() -> validator.isValid(null, context))
        .isInstanceOf(NullPointerException.class);
  }
}
