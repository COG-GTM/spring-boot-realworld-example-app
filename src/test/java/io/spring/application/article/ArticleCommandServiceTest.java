package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleCommandServiceTest {

  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_article_with_valid_params() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article result = articleCommandService.createArticle(param, user);

    assertNotNull(result);
    assertEquals("Test Title", result.getTitle());
    assertEquals("Test Description", result.getDescription());
    assertEquals("Test Body", result.getBody());
    assertEquals(user.getId(), result.getUserId());
    assertEquals(2, result.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_with_empty_tags() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(Collections.emptyList())
            .build();

    Article result = articleCommandService.createArticle(param, user);

    assertNotNull(result);
    assertEquals(0, result.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_with_null_tags() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(null)
            .build();

    assertThrows(
        Exception.class,
        () -> {
          articleCommandService.createArticle(param, user);
        });
  }

  @Test
  public void should_update_article_with_valid_params() {
    Article article =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("tag"), "user1");
    UpdateArticleParam param = new UpdateArticleParam("New Title", "New Body", "New Desc");

    Article result = articleCommandService.updateArticle(article, param);

    assertNotNull(result);
    assertEquals("New Title", result.getTitle());
    assertEquals("New Body", result.getBody());
    assertEquals("New Desc", result.getDescription());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_with_empty_params() {
    Article article =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("tag"), "user1");
    UpdateArticleParam param = new UpdateArticleParam();

    Article result = articleCommandService.updateArticle(article, param);

    assertNotNull(result);
    assertEquals("Old Title", result.getTitle());
    assertEquals("Old Desc", result.getDescription());
    assertEquals("Old Body", result.getBody());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_only_title() {
    Article article =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("tag"), "user1");
    UpdateArticleParam param = new UpdateArticleParam("New Title", "", "");

    Article result = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", result.getTitle());
    assertEquals("new-title", result.getSlug());
    assertEquals("Old Desc", result.getDescription());
    assertEquals("Old Body", result.getBody());
  }

  @Test
  public void should_update_only_body() {
    Article article =
        new Article("Title", "Desc", "Old Body", Arrays.asList("tag"), "user1");
    UpdateArticleParam param = new UpdateArticleParam("", "New Body", "");

    Article result = articleCommandService.updateArticle(article, param);

    assertEquals("Title", result.getTitle());
    assertEquals("New Body", result.getBody());
    assertEquals("Desc", result.getDescription());
  }

  @Test
  public void should_update_only_description() {
    Article article =
        new Article("Title", "Old Desc", "Body", Arrays.asList("tag"), "user1");
    UpdateArticleParam param = new UpdateArticleParam("", "", "New Desc");

    Article result = articleCommandService.updateArticle(article, param);

    assertEquals("Title", result.getTitle());
    assertEquals("Body", result.getBody());
    assertEquals("New Desc", result.getDescription());
  }
}
