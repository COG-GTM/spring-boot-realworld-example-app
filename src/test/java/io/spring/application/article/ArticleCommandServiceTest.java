package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    creator = new User("creator@example.com", "creator", "123", "", "");
  }

  @Test
  public void should_create_article_from_new_article_param() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to learn Spring Boot")
            .description("a description")
            .body("the body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTitle()).isEqualTo("How to learn Spring Boot");
    assertThat(article.getDescription()).isEqualTo("a description");
    assertThat(article.getBody()).isEqualTo("the body");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_generate_slug_from_title_on_create() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("A New Title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getSlug()).isEqualTo("a-new-title");
  }

  @Test
  public void should_map_tag_list_to_tags_and_deduplicate() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("tags")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    List<String> tagNames =
        article.getTags().stream()
            .map(t -> t.getName())
            .collect(java.util.stream.Collectors.toList());
    assertThat(tagNames).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_handle_empty_tag_list_on_create() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("no tags")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTags()).isEmpty();
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_title_description_and_body() {
    Article article =
        new Article("old title", "old desc", "old body", Collections.emptyList(), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getDescription()).isEqualTo("new desc");
    assertThat(updated.getBody()).isEqualTo("new body");
    verify(articleRepository).save(article);
  }

  @Test
  public void should_regenerate_slug_when_title_updated() {
    Article article =
        new Article("old title", "old desc", "old body", Collections.emptyList(), creator.getId());
    assertThat(article.getSlug()).isEqualTo("old-title");
    UpdateArticleParam param = new UpdateArticleParam("Brand New Title", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getSlug()).isEqualTo("brand-new-title");
  }

  @Test
  public void should_not_change_fields_when_update_params_are_empty() {
    Article article =
        new Article("old title", "old desc", "old body", Collections.emptyList(), creator.getId());
    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle()).isEqualTo("old title");
    assertThat(updated.getDescription()).isEqualTo("old desc");
    assertThat(updated.getBody()).isEqualTo("old body");
    assertThat(updated.getSlug()).isEqualTo("old-title");
    verify(articleRepository, times(1)).save(article);
  }
}
