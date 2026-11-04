package io.spring.application.article;

import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {
  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;
  private User user;

  @BeforeEach
  public void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
    user = new User("a@test.com", "a", "123", "", "");
  }

  @Test
  public void should_create_article_for_creator() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("my title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    Assertions.assertEquals("my title", article.getTitle());
    Assertions.assertEquals("my-title", article.getSlug());
    Assertions.assertEquals("desc", article.getDescription());
    Assertions.assertEquals("body", article.getBody());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertTrue(article.getTags().contains(new Tag("java")));
    Assertions.assertTrue(article.getTags().contains(new Tag("spring")));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_article_and_save() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", ""));

    Assertions.assertSame(article, updated);
    Assertions.assertEquals("new title", updated.getTitle());
    Assertions.assertEquals("new-title", updated.getSlug());
    Assertions.assertEquals("new body", updated.getBody());
    Assertions.assertEquals("desc", updated.getDescription());
    verify(articleRepository).save(article);
  }
}
