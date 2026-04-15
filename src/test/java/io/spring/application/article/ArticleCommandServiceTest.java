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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;

  @BeforeEach
  void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  void should_create_article_successfully() {
    User user = new User("test@example.com", "testuser", "password", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test description")
            .body("Test body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertNotNull(article);
    assertEquals("Test Article", article.getTitle());
    assertEquals("test-article", article.getSlug());
    assertEquals("Test description", article.getDescription());
    assertEquals("Test body", article.getBody());
    assertEquals(user.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_create_article_with_empty_tags() {
    User user = new User("test@example.com", "testuser", "password", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test description")
            .body("Test body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertNotNull(article);
    assertEquals(0, article.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_update_article_successfully() {
    Article article =
        new Article("Old Title", "old desc", "old body", Collections.emptyList(), "userId1");
    UpdateArticleParam param = new UpdateArticleParam("New Title", "new body", "new desc");

    Article updatedArticle = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", updatedArticle.getTitle());
    assertEquals("new-title", updatedArticle.getSlug());
    assertEquals("new desc", updatedArticle.getDescription());
    assertEquals("new body", updatedArticle.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_with_partial_params() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "userId1");
    UpdateArticleParam param = new UpdateArticleParam("New Title", null, null);

    Article updatedArticle = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", updatedArticle.getTitle());
    assertEquals("desc", updatedArticle.getDescription());
    assertEquals("body", updatedArticle.getBody());
    verify(articleRepository).save(article);
  }
}
