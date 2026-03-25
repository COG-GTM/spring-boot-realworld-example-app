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
  public void should_create_article_and_save() {
    User user = new User("a@b.com", "user", "pass", "", "");
    NewArticleParam param = NewArticleParam.builder()
        .title("Test Article")
        .description("desc")
        .body("body content")
        .tagList(Arrays.asList("java", "spring"))
        .build();
    Article article = articleCommandService.createArticle(param, user);
    assertNotNull(article);
    assertEquals("test-article", article.getSlug());
    assertEquals("Test Article", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body content", article.getBody());
    assertEquals(user.getId(), article.getUserId());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_with_empty_tags() {
    User user = new User("a@b.com", "user", "pass", "", "");
    NewArticleParam param = NewArticleParam.builder()
        .title("No Tags")
        .description("desc")
        .body("body")
        .tagList(Collections.emptyList())
        .build();
    Article article = articleCommandService.createArticle(param, user);
    assertTrue(article.getTags().isEmpty());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_and_save() {
    Article article = new Article("Old Title", "old desc", "old body", Collections.emptyList(), "user1");
    UpdateArticleParam param = new UpdateArticleParam("New Title", "new body", "new desc");
    Article updated = articleCommandService.updateArticle(article, param);
    assertEquals("New Title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_article_with_empty_fields() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1");
    UpdateArticleParam param = new UpdateArticleParam("", "", "");
    Article updated = articleCommandService.updateArticle(article, param);
    assertEquals("Title", updated.getTitle());
    assertEquals("desc", updated.getDescription());
    assertEquals("body", updated.getBody());
    verify(articleRepository).save(article);
  }
}
