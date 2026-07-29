package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  @InjectMocks private DuplicatedArticleValidator validator;

  private AutoCloseable mocks;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void should_be_valid_when_no_article_found_with_the_slug() {
    when(articleQueryService.findBySlug(eq("a-new-title"), any())).thenReturn(Optional.empty());

    assertThat(validator.isValid("a new title", null), is(true));
  }

  @Test
  public void should_be_invalid_when_an_article_already_uses_the_slug() {
    User user = new User("john@example.com", "john", "123", "", "");
    when(articleQueryService.findBySlug(eq("a-new-title"), any()))
        .thenReturn(Optional.of(TestHelper.articleDataFixture("test", user)));

    assertThat(validator.isValid("a new title", null), is(false));
  }

  @Test
  public void should_look_up_with_the_slugified_title_not_the_raw_title() {
    when(articleQueryService.findBySlug(any(), any())).thenReturn(Optional.empty());

    validator.isValid("A NEW   Title?", null);

    verify(articleQueryService).findBySlug("a-new-title-", null);
    verify(articleQueryService, never()).findBySlug(eq("A NEW   Title?"), any());
  }

  @Test
  public void should_pass_null_user_to_the_query_service() {
    when(articleQueryService.findBySlug(any(), any())).thenReturn(Optional.empty());

    validator.isValid("title", null);

    verify(articleQueryService).findBySlug("title", null);
  }

  @Test
  public void should_be_valid_for_empty_title_when_nothing_matches() {
    when(articleQueryService.findBySlug(eq(""), any())).thenReturn(Optional.empty());

    assertThat(validator.isValid("", null), is(true));

    verify(articleQueryService).findBySlug("", null);
  }

  @Test
  public void should_throw_on_null_title() {
    assertThrows(NullPointerException.class, () -> validator.isValid(null, null));

    verify(articleQueryService, never()).findBySlug(any(), any());
  }
}
