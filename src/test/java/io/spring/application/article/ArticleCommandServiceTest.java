package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  private User creator;

  @BeforeEach
  void setUp() {
    creator = new User("test@example.com", "testuser", "password", "", "");
  }

  @Test
  void should_create_article_successfully() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertNotNull(article);
    assertEquals("Test Title", article.getTitle());
    assertEquals("Test Description", article.getDescription());
    assertEquals("Test Body", article.getBody());
    assertEquals("test-title", article.getSlug());
    assertEquals(creator.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No Tags Article")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertNotNull(article);
    assertTrue(article.getTags().isEmpty());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_update_article_title() {
    Article article =
        new Article("Original Title", "desc", "body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("New Title", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("desc", updated.getDescription());
    assertEquals("body", updated.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_with_partial_update_body_only() {
    Article article =
        new Article("Title", "desc", "original body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "new body", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("Title", updated.getTitle());
    assertEquals("desc", updated.getDescription());
    assertEquals("new body", updated.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_with_partial_update_description_only() {
    Article article =
        new Article("Title", "original desc", "body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("Title", updated.getTitle());
    assertEquals("new desc", updated.getDescription());
    assertEquals("body", updated.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_with_all_fields() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("New Title", "New Body", "New Desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", updated.getTitle());
    assertEquals("New Desc", updated.getDescription());
    assertEquals("New Body", updated.getBody());
    assertEquals("new-title", updated.getSlug());
    verify(articleRepository).save(article);
  }
}
