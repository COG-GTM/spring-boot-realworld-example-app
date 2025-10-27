package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  private User testUser;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "encoded123", "Test bio", "http://image.url");
  }

  @Test
  public void should_create_article_with_valid_parameters() {
    String title = "Test Article";
    String description = "Test Description";
    String body = "Test Body";
    List<String> tags = Arrays.asList("java", "spring");
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title(title)
            .description(description)
            .body(body)
            .tagList(tags)
            .build();

    Article result = articleCommandService.createArticle(newArticleParam, testUser);

    assertNotNull(result);
    assertEquals(title, result.getTitle());
    assertEquals(description, result.getDescription());
    assertEquals(body, result.getBody());
    assertEquals(testUser.getId(), result.getUserId());
    assertNotNull(result.getId());
    assertNotNull(result.getSlug());
    assertEquals(2, result.getTags().size());

    ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(articleCaptor.capture());
    Article savedArticle = articleCaptor.getValue();
    assertEquals(title, savedArticle.getTitle());
  }

  @Test
  public void should_create_article_without_tags() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test Description")
            .body("Test Body")
            .tagList(Collections.emptyList())
            .build();

    Article result = articleCommandService.createArticle(newArticleParam, testUser);

    assertNotNull(result);
    assertTrue(result.getTags().isEmpty());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_generate_correct_slug_when_creating_article() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("How to Train Your Dragon")
            .description("Description")
            .body("Body")
            .tagList(Collections.emptyList())
            .build();

    Article result = articleCommandService.createArticle(newArticleParam, testUser);

    assertEquals("how-to-train-your-dragon", result.getSlug());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_title() {
    Article existingArticle =
        new Article(
            "Old Title", "Old Description", "Old Body", Arrays.asList("tag1"), testUser.getId());
    UpdateArticleParam updateArticleParam = new UpdateArticleParam("New Title", "", "");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertEquals("New Title", result.getTitle());
    assertEquals("new-title", result.getSlug());
    assertEquals("Old Description", result.getDescription());
    assertEquals("Old Body", result.getBody());
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_article_description() {
    Article existingArticle =
        new Article("Title", "Old Description", "Old Body", Arrays.asList("tag1"), testUser.getId());
    UpdateArticleParam updateArticleParam = new UpdateArticleParam("", "", "New Description");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertEquals("Title", result.getTitle());
    assertEquals("New Description", result.getDescription());
    assertEquals("Old Body", result.getBody());
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_article_body() {
    Article existingArticle =
        new Article(
            "Title", "Description", "Old Body", Arrays.asList("tag1"), testUser.getId());
    UpdateArticleParam updateArticleParam = new UpdateArticleParam("", "New Body", "");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertEquals("Title", result.getTitle());
    assertEquals("Description", result.getDescription());
    assertEquals("New Body", result.getBody());
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_multiple_fields() {
    Article existingArticle =
        new Article(
            "Old Title",
            "Old Description",
            "Old Body",
            Arrays.asList("tag1"),
            testUser.getId());
    UpdateArticleParam updateArticleParam =
        new UpdateArticleParam("New Title", "New Body", "New Description");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertEquals("New Title", result.getTitle());
    assertEquals("new-title", result.getSlug());
    assertEquals("New Description", result.getDescription());
    assertEquals("New Body", result.getBody());
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_not_update_with_empty_fields() {
    Article existingArticle =
        new Article("Title", "Description", "Body", Arrays.asList("tag1"), testUser.getId());
    UpdateArticleParam updateArticleParam = new UpdateArticleParam("", "", "");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertEquals("Title", result.getTitle());
    assertEquals("Description", result.getDescription());
    assertEquals("Body", result.getBody());
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_slug_when_title_changes() {
    Article existingArticle =
        new Article("Old Title", "Description", "Body", Arrays.asList("tag1"), testUser.getId());
    String oldSlug = existingArticle.getSlug();
    UpdateArticleParam updateArticleParam = new UpdateArticleParam("Completely New Title", "", "");

    Article result = articleCommandService.updateArticle(existingArticle, updateArticleParam);

    assertNotEquals(oldSlug, result.getSlug());
    assertEquals("completely-new-title", result.getSlug());
    verify(articleRepository).save(existingArticle);
  }
}
