package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleCommandServiceTest {

  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;
  private User user;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
    user = new User("jane@example.com", "jane", "secret", "", "");
  }

  @Test
  public void should_create_article_and_save() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How To Test")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "testing"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertEquals("How To Test", article.getTitle());
    assertEquals("how-to-test", article.getSlug());
    assertEquals(user.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_and_save() {
    Article article =
        new Article("Old Title", "old desc", "old body", Arrays.asList("tag"), user.getId());
    UpdateArticleParam param = new UpdateArticleParam("New Title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new desc", updated.getDescription());
    assertEquals("new body", updated.getBody());
    verify(articleRepository).save(article);
  }
}
