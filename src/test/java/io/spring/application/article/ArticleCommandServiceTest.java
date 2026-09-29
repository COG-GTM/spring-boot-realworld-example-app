package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private ArticleCommandService service;
  private User user;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    service = new ArticleCommandService(articleRepository);
    user = new User("a@b.com", "user", "pass", "", "");
  }

  @Test
  public void should_create_and_save_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("My Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();

    Article article = service.createArticle(param, user);

    assertEquals("my-title", article.getSlug());
    assertEquals(user.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_and_save_article() {
    Article article = new Article("old", "old desc", "old body", Arrays.asList("java"), "u");

    Article updated =
        service.updateArticle(article, new UpdateArticleParam("new title", "new body", ""));

    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new body", updated.getBody());
    assertEquals("old desc", updated.getDescription());
    verify(articleRepository).save(article);
  }
}
