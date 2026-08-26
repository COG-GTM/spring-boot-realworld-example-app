package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
  private User user;

  @BeforeEach
  public void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
    user = new User("email@test.com", "username", "123", "", "");
  }

  @Test
  public void should_create_and_save_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertThat(article.getTitle(), is("a title"));
    assertThat(article.getSlug(), is("a-title"));
    assertThat(article.getUserId(), is(user.getId()));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_and_save_article() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), user.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle(), is("new title"));
    assertThat(updated.getSlug(), is("new-title"));
    assertThat(updated.getDescription(), is("new desc"));
    assertThat(updated.getBody(), is("new body"));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_ignore_empty_fields_on_update() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), user.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle(), is("old title"));
    assertThat(updated.getDescription(), is("old desc"));
    assertThat(updated.getBody(), is("old body"));
  }
}
