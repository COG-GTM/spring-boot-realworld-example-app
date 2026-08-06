package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.spring.TestHelper;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;
  @Captor private ArgumentCaptor<Article> articleCaptor;

  private ArticleCommandService articleCommandService;
  private User creator;

  @BeforeEach
  void setUp() {
    articleCommandService = new ArticleCommandService(articleRepository);
    creator = TestHelper.userFixture("john");
  }

  @Test
  void should_create_article_with_slug_tags_and_author() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to train your Dragon")
            .description("Ever wonder how?")
            .body("You have to believe")
            .tagList(Arrays.asList("reactjs", "angularjs", "dragons"))
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    verify(articleRepository).save(articleCaptor.capture());
    Article saved = articleCaptor.getValue();
    assertThat(saved).isSameAs(created);
    assertThat(saved.getId()).isNotBlank();
    assertThat(saved.getSlug()).isEqualTo("how-to-train-your-dragon");
    assertThat(saved.getTitle()).isEqualTo("How to train your Dragon");
    assertThat(saved.getDescription()).isEqualTo("Ever wonder how?");
    assertThat(saved.getBody()).isEqualTo("You have to believe");
    assertThat(tagNames(saved)).containsExactlyInAnyOrder("reactjs", "angularjs", "dragons");
    assertThat(saved.getUserId()).isEqualTo(creator.getId());
    assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
  }

  @Test
  void should_deduplicate_tags_when_creating_article() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Tagged")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(tagNames(created)).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  void should_create_article_without_tags() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No tags here")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article created = articleCommandService.createArticle(param, creator);

    assertThat(created.getTags()).isEmpty();
    verify(articleRepository).save(created);
  }

  @Test
  void should_update_title_description_and_body() {
    Article article = TestHelper.articleFixture("old", creator);

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("New Title", "new body", "new desc"));

    verify(articleRepository).save(article);
    assertThat(updated).isSameAs(article);
    assertThat(article.getTitle()).isEqualTo("New Title");
    assertThat(article.getSlug()).isEqualTo("new-title");
    assertThat(article.getBody()).isEqualTo("new body");
    assertThat(article.getDescription()).isEqualTo("new desc");
  }

  @Test
  void should_leave_article_unchanged_when_update_param_is_empty() {
    Article article = TestHelper.articleFixture("old", creator);
    String slug = article.getSlug();

    articleCommandService.updateArticle(article, new UpdateArticleParam("", "", ""));

    verify(articleRepository).save(article);
    assertThat(article.getTitle()).isEqualTo("title old");
    assertThat(article.getSlug()).isEqualTo(slug);
    assertThat(article.getBody()).isEqualTo("body old");
    assertThat(article.getDescription()).isEqualTo("desc old");
  }

  @Test
  void should_only_update_provided_fields() {
    Article article = TestHelper.articleFixture("old", creator);

    articleCommandService.updateArticle(article, new UpdateArticleParam("", "updated body", ""));

    assertThat(article.getBody()).isEqualTo("updated body");
    assertThat(article.getTitle()).isEqualTo("title old");
    assertThat(article.getDescription()).isEqualTo("desc old");
  }

  private static List<String> tagNames(Article article) {
    return article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
  }
}
