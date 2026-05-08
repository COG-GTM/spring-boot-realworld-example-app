package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
  private User creator;

  @BeforeEach
  public void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
    creator = new User("a@b.com", "alice", "secret", "", "");
  }

  @Test
  public void createArticle_should_build_article_and_save() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertNotNull(article);
    assertEquals("title", article.getTitle());
    assertEquals("title", article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(creator.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository, times(1)).save(any(Article.class));
  }

  @Test
  public void createArticle_should_handle_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertEquals(0, article.getTags().size());
    verify(articleRepository, times(1)).save(any(Article.class));
  }

  @Test
  public void updateArticle_should_apply_param_and_save() throws InterruptedException {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    String originalId = article.getId();
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Thread.sleep(2);
    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals(originalId, updated.getId());
    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new desc", updated.getDescription());
    assertEquals("new body", updated.getBody());
    assertTrue(updated.getUpdatedAt().getMillis() >= updated.getCreatedAt().getMillis());
    verify(articleRepository, times(1)).save(article);
  }

  @Test
  public void updateArticle_should_ignore_blank_fields() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("title", updated.getTitle());
    assertEquals("desc", updated.getDescription());
    assertEquals("body", updated.getBody());
    verify(articleRepository, times(1)).save(article);
  }
}
