package io.spring.application.article;

import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  @Test
  public void should_create_article_and_save() {
    User user = new User("email@example.com", "username", "123", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    verify(articleRepository).save(article);
    Assertions.assertEquals("Test Title", article.getTitle());
    Assertions.assertEquals("test-title", article.getSlug());
    Assertions.assertEquals(user.getId(), article.getUserId());
    Assertions.assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_update_article_and_save() {
    User user = new User("email@example.com", "username", "123", "", "");
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList(), user.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    verify(articleRepository).save(article);
    Assertions.assertEquals("new title", updated.getTitle());
    Assertions.assertEquals("new-title", updated.getSlug());
    Assertions.assertEquals("new desc", updated.getDescription());
    Assertions.assertEquals("new body", updated.getBody());
  }
}
