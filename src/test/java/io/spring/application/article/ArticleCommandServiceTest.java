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
    creator = new User("test@example.com", "testuser", "password", "bio", "image");
  }

  @Test
  public void should_create_article_with_valid_params() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Test Article")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article, notNullValue());
    assertThat(article.getTitle(), is("Test Article"));
    assertThat(article.getDescription(), is("Test Description"));
    assertThat(article.getBody(), is("Test Body"));
    assertThat(article.getUserId(), is(creator.getId()));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_without_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article Without Tags")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article, notNullValue());
    assertThat(article.getTitle(), is("Article Without Tags"));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Article With Empty Tags")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article, notNullValue());
    assertThat(article.getTags().size(), is(0));
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_generate_slug_from_title() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("My New Article Title")
            .description("Description")
            .body("Body")
            .tagList(Arrays.asList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getSlug(), is("my-new-article-title"));
  }

  @Test
  public void should_update_article_title() {
    Article existingArticle =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getTitle(), is("New Title"));
    assertThat(updatedArticle.getSlug(), is("new-title"));
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_article_description() {
    Article existingArticle =
        new Article("Title", "Old Description", "Body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "New Description");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getDescription(), is("New Description"));
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_article_body() {
    Article existingArticle =
        new Article("Title", "Description", "Old Body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("", "New Body", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getBody(), is("New Body"));
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_update_multiple_fields() {
    Article existingArticle =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam updateParam =
        new UpdateArticleParam("New Title", "New Body", "New Description");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getTitle(), is("New Title"));
    assertThat(updatedArticle.getBody(), is("New Body"));
    assertThat(updatedArticle.getDescription(), is("New Description"));
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_not_update_fields_when_empty() {
    Article existingArticle =
        new Article(
            "Original Title",
            "Original Description",
            "Original Body",
            Arrays.asList("java"),
            creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("", "", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getTitle(), is("Original Title"));
    assertThat(updatedArticle.getDescription(), is("Original Description"));
    assertThat(updatedArticle.getBody(), is("Original Body"));
    verify(articleRepository).save(existingArticle);
  }

  @Test
  public void should_preserve_article_id_after_update() {
    Article existingArticle =
        new Article("Title", "Description", "Body", Arrays.asList("java"), creator.getId());
    String originalId = existingArticle.getId();
    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getId(), is(originalId));
  }

  @Test
  public void should_preserve_article_user_id_after_update() {
    Article existingArticle =
        new Article("Title", "Description", "Body", Arrays.asList("java"), creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getUserId(), is(creator.getId()));
  }

  @Test
  public void should_preserve_article_tags_after_update() {
    Article existingArticle =
        new Article(
            "Title", "Description", "Body", Arrays.asList("java", "spring"), creator.getId());
    UpdateArticleParam updateParam = new UpdateArticleParam("New Title", "", "");

    Article updatedArticle = articleCommandService.updateArticle(existingArticle, updateParam);

    assertThat(updatedArticle.getTags().size(), is(2));
  }
}
