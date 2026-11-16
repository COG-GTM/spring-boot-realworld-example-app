package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {ArticleCommandService.class, ValidationAutoConfiguration.class})
public class ArticleCommandServiceValidationTest {
  @Autowired private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  private final User user = new User("john@jacob.com", "johnjacob", "123", "", "");

  @Test
  public void should_reject_blank_fields() {
    when(articleQueryService.findBySlug(anyString(), isNull())).thenReturn(Optional.empty());
    NewArticleParam param =
        NewArticleParam.builder().title("").description("").body("").tagList(null).build();

    ConstraintViolationException e =
        assertThrows(
            ConstraintViolationException.class,
            () -> articleCommandService.createArticle(param, user));

    Map<String, String> messages = messagesByLeafProperty(e);
    assertEquals("can't be empty", messages.get("title"));
    assertEquals("can't be empty", messages.get("description"));
    assertEquals("can't be empty", messages.get("body"));
    verify(articleRepository, never()).save(any());
  }

  @Test
  public void should_reject_duplicated_article_title() {
    when(articleQueryService.findBySlug(eq("existing-title"), isNull()))
        .thenReturn(Optional.of(new ArticleData()));
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Existing Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    ConstraintViolationException e =
        assertThrows(
            ConstraintViolationException.class,
            () -> articleCommandService.createArticle(param, user));

    assertEquals("article name exists", messagesByLeafProperty(e).get("title"));
    verify(articleRepository, never()).save(any());
  }

  @Test
  public void should_create_valid_article() {
    when(articleQueryService.findBySlug(eq("fresh-title"), isNull())).thenReturn(Optional.empty());
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Fresh Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    articleCommandService.createArticle(param, user);

    verify(articleRepository).save(any());
  }

  private static Map<String, String> messagesByLeafProperty(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .collect(
            Collectors.toMap(v -> leaf(v), ConstraintViolation::getMessage, (a, b) -> a + "," + b));
  }

  private static String leaf(ConstraintViolation<?> v) {
    String path = v.getPropertyPath().toString();
    return path.substring(path.lastIndexOf('.') + 1);
  }
}
