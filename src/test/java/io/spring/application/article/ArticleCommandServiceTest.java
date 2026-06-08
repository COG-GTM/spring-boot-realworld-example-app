package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
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
  void createArticle_should_save_and_return_article() {
    User creator = new User("e@t.com", "user", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article result = articleCommandService.createArticle(param, creator);

    assertNotNull(result);
    assertEquals("Test Title", result.getTitle());
    assertEquals("Test Description", result.getDescription());
    assertEquals("Test Body", result.getBody());
    assertEquals(creator.getId(), result.getUserId());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void updateArticle_should_update_and_save() {
    User creator = new User("e@t.com", "user", "pass", "", "");
    Article article =
        new Article("Old Title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("New Title", "new body", "new desc");

    Article result = articleCommandService.updateArticle(article, param);

    assertEquals("New Title", result.getTitle());
    assertEquals("new-title", result.getSlug());
    verify(articleRepository).save(article);
  }
}
