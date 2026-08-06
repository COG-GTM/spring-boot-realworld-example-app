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
  private ArticleCommandService articleCommandService;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  void should_create_article_and_save() {
    User creator = new User("me@example.com", "me", "123", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertEquals("a-new-title", article.getSlug());
    assertEquals(creator.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_fields() {
    User creator = new User("me@example.com", "me", "123", "", "");
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new body", updated.getBody());
    assertEquals("new desc", updated.getDescription());
    verify(articleRepository).save(article);
  }
}
