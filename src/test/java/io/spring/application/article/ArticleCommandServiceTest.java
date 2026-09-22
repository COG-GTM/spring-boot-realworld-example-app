package io.spring.application.article;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;
  private final User user = new User("email@test.com", "username", "123", "", "");

  @BeforeEach
  public void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_and_save_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a title")
            .description("desc")
            .body("body")
            .tagList(singletonList("java"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertEquals("a title", article.getTitle());
    assertEquals("a-title", article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(1, article.getTags().size());
    assertEquals(user.getId(), article.getUserId());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_and_save_article() {
    Article article =
        new Article("old title", "old desc", "old body", singletonList("java"), user.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new desc"));

    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new desc", updated.getDescription());
    assertEquals("new body", updated.getBody());
    verify(articleRepository).save(article);
  }
}
