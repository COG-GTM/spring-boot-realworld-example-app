package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
    creator = new User("aisensiy@gmail.com", "aisensiy", "123", "", "");
  }

  @Test
  public void should_create_article_from_new_article_param() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, creator);

    assertThat(article.getTitle()).isEqualTo("a new title");
    assertThat(article.getDescription()).isEqualTo("desc");
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    assertThat(tagNamesOf(article)).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_create_article_with_slug_from_title() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("How To Train Your Dragon")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("dragons"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, creator);

    assertThat(article.getSlug()).isEqualTo("how-to-train-your-dragon");
  }

  @Test
  public void should_deduplicate_tags_of_new_article() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, creator);

    assertThat(tagNamesOf(article)).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_save_created_article_into_repository() {
    NewArticleParam newArticleParam =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    Article article = articleCommandService.createArticle(newArticleParam, creator);

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue()).isEqualTo(article);
  }

  @Test
  public void should_update_article_and_refresh_slug() {
    Article article = existingArticle();
    UpdateArticleParam updateArticleParam =
        new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, updateArticleParam);

    assertThat(updated).isSameAs(article);
    assertThat(article.getTitle()).isEqualTo("new title");
    assertThat(article.getDescription()).isEqualTo("new desc");
    assertThat(article.getBody()).isEqualTo("new body");
    assertThat(article.getSlug()).isEqualTo("new-title");
    verify(articleRepository, times(1)).save(article);
  }

  @Test
  public void should_keep_original_values_when_update_param_is_empty() {
    Article article = existingArticle();
    String originalSlug = article.getSlug();

    articleCommandService.updateArticle(article, new UpdateArticleParam("", "", ""));

    assertThat(article.getTitle()).isEqualTo("old title");
    assertThat(article.getDescription()).isEqualTo("old desc");
    assertThat(article.getBody()).isEqualTo("old body");
    assertThat(article.getSlug()).isEqualTo(originalSlug);
    verify(articleRepository, times(1)).save(any(Article.class));
  }

  @Test
  public void should_update_only_provided_fields() {
    Article article = existingArticle();

    articleCommandService.updateArticle(article, new UpdateArticleParam("", "brand new body", ""));

    assertThat(article.getTitle()).isEqualTo("old title");
    assertThat(article.getDescription()).isEqualTo("old desc");
    assertThat(article.getBody()).isEqualTo("brand new body");
    assertThat(article.getSlug()).isEqualTo("old-title");
  }

  private Article existingArticle() {
    return new Article("old title", "old desc", "old body", Arrays.asList("java"), creator.getId());
  }

  private List<String> tagNamesOf(Article article) {
    return article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
  }
}
