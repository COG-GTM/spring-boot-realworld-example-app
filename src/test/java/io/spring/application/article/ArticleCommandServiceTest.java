package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  private User creator;

  @BeforeEach
  public void setUp() {
    creator = new User("author@example.com", "author", "123", "", "");
  }

  @Test
  public void should_create_article_from_param_and_save_it() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertEquals("a new title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals("a-new-title", article.getSlug());
    assertEquals(creator.getId(), article.getUserId());
    assertEquals(2, article.getTags().size());

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository, times(1)).save(captor.capture());
    assertSame(article, captor.getValue());
  }

  @Test
  public void should_update_article_fields_and_save_it() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertSame(article, updated);
    assertEquals("new title", updated.getTitle());
    assertEquals("new desc", updated.getDescription());
    assertEquals("new body", updated.getBody());
    assertEquals("new-title", updated.getSlug());
    verify(articleRepository, times(1)).save(article);
  }

  @Test
  public void should_keep_original_fields_when_update_param_is_empty() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals("old title", updated.getTitle());
    assertEquals("old desc", updated.getDescription());
    assertEquals("old body", updated.getBody());
    verify(articleRepository, times(1)).save(article);
  }
}
