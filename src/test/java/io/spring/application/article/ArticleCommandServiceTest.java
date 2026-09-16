package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
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
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @Test
  public void should_create_article_for_user_and_persist() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("New Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(user.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    assertTrue(article.getTags().contains(new Tag("java")));
    assertTrue(article.getTags().contains(new Tag("spring")));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_article_and_persist() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("Updated", "new body", ""));

    assertSame(article, updated);
    assertEquals("Updated", updated.getTitle());
    assertEquals("updated", updated.getSlug());
    assertEquals("new body", updated.getBody());
    assertEquals("desc", updated.getDescription());
    verify(articleRepository).save(article);
  }
}
