package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {
  @Mock private ArticleRepository articleRepository;

  private ArticleCommandService articleCommandService;

  @BeforeEach
  public void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_article() {
    User creator = new User("email", "username", "password", "bio", "image");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertEquals("Title", article.getTitle());
    assertEquals("Description", article.getDescription());
    assertEquals("Body", article.getBody());
    assertEquals(creator.getId(), article.getUserId());
    assertEquals(
        Arrays.asList("java", "spring"),
        article.getTags().stream()
            .map(tag -> tag.getName())
            .sorted()
            .collect(java.util.stream.Collectors.toList()));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_article() {
    Article article =
        new Article("Old title", "Old description", "Old body", Arrays.asList("java"), "user");
    UpdateArticleParam param = new UpdateArticleParam("New title", "New body", "New description");

    Article updated = articleCommandService.updateArticle(article, param);

    assertEquals(article, updated);
    assertEquals("New title", article.getTitle());
    assertEquals("New description", article.getDescription());
    assertEquals("New body", article.getBody());
    assertEquals("new-title", article.getSlug());
    verify(articleRepository).save(article);
  }
}
