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
import org.mockito.Mockito;

public class ArticleCommandServiceTest {

  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;

  @BeforeEach
  public void setUp() {
    articleRepository = Mockito.mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
  }

  @Test
  public void should_create_article_and_save() {
    User creator = new User("e@e.com", "creator", "pass", "", "");
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
    assertThat(article.getUserId(), is(creator.getId()));
    assertThat(article.getSlug(), is("test-title"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_and_save() {
    User creator = new User("e@e.com", "creator", "pass", "", "");
    Article article =
        new Article(
            "Original Title",
            "Original Desc",
            "Original Body",
            Arrays.asList("java"),
            creator.getId());

    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "New Body", "New Desc");

    Article updated = articleCommandService.updateArticle(article, updateParam);

    assertThat(updated.getTitle(), is("New Title"));
    assertThat(updated.getBody(), is("New Body"));
    assertThat(updated.getDescription(), is("New Desc"));
    assertThat(updated.getSlug(), is("new-title"));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    User creator = new User("e@e.com", "creator", "pass", "", "");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No Tags")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTags().size(), is(0));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_not_update_article_fields_when_empty() {
    User creator = new User("e@e.com", "creator", "pass", "", "");
    Article article =
        new Article(
            "Original Title",
            "Original Desc",
            "Original Body",
            Arrays.asList("java"),
            creator.getId());

    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, updateParam);

    assertThat(updated.getTitle(), is("Original Title"));
    assertThat(updated.getDescription(), is("Original Desc"));
    assertThat(updated.getBody(), is("Original Body"));
    verify(articleRepository).save(article);
  }
}
