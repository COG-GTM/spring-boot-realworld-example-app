package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
  public void should_create_article_and_save() {
    User creator = new User("test@test.com", "testuser", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article, notNullValue());
    assertThat(article.getTitle(), is("Test Title"));
    assertThat(article.getDescription(), is("Test Description"));
    assertThat(article.getBody(), is("Test Body"));
    assertThat(article.getSlug(), is("test-title"));
    assertThat(article.getUserId(), is(creator.getId()));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_title() {
    User creator = new User("test@test.com", "testuser", "pass", "", "");
    Article article =
        new Article("Original Title", "desc", "body", Arrays.asList("java"), creator.getId());

    UpdateArticleParam param = new UpdateArticleParam("New Title", "", "");
    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle(), is("New Title"));
    assertThat(updated.getSlug(), is("new-title"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_body() {
    User creator = new User("test@test.com", "testuser", "pass", "", "");
    Article article =
        new Article("Title", "desc", "original body", Arrays.asList("java"), creator.getId());

    UpdateArticleParam param = new UpdateArticleParam("", "new body", "");
    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getBody(), is("new body"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_description() {
    User creator = new User("test@test.com", "testuser", "pass", "", "");
    Article article =
        new Article("Title", "old desc", "body", Arrays.asList("java"), creator.getId());

    UpdateArticleParam param = new UpdateArticleParam("", "", "new desc");
    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getDescription(), is("new desc"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    User creator = new User("test@test.com", "testuser", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No Tags Article")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTags().size(), is(0));
    verify(articleRepository).save(any(Article.class));
  }
}
