package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

  private User creator() {
    return new User("john@example.com", "john", "123", "bio", "image");
  }

  @Test
  public void should_create_article_and_save_it() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    User creator = creator();
    NewArticleParam param =
        NewArticleParam.builder()
            .title("a new title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = service.createArticle(param, creator);

    verify(articleRepository).save(articleCaptor.capture());
    assertThat(articleCaptor.getValue()).isSameAs(article);
    assertThat(article.getTitle()).isEqualTo("a new title");
    assertThat(article.getDescription()).isEqualTo("desc");
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getSlug()).isEqualTo("a-new-title");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    assertThat(article.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);

    Article article =
        service.createArticle(
            NewArticleParam.builder()
                .title("no tags")
                .description("desc")
                .body("body")
                .tagList(Collections.emptyList())
                .build(),
            creator());

    verify(articleRepository).save(article);
    List<Tag> tags = article.getTags();
    assertThat(tags).isEmpty();
  }

  @Test
  public void should_update_all_fields_of_article() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "user-id");

    Article updated =
        service.updateArticle(article, new UpdateArticleParam("new title", "new body", "new desc"));

    verify(articleRepository).save(articleCaptor.capture());
    assertThat(articleCaptor.getValue()).isSameAs(updated);
    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getBody()).isEqualTo("new body");
    assertThat(updated.getDescription()).isEqualTo("new desc");
    assertThat(updated.getSlug()).isEqualTo("new-title");
  }

  @Test
  public void should_only_update_provided_fields() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "user-id");

    Article updated = service.updateArticle(article, new UpdateArticleParam("", "new body", ""));

    verify(articleRepository).save(updated);
    assertThat(updated.getTitle()).isEqualTo("old title");
    assertThat(updated.getSlug()).isEqualTo("old-title");
    assertThat(updated.getDescription()).isEqualTo("old desc");
    assertThat(updated.getBody()).isEqualTo("new body");
  }

  @Test
  public void should_keep_article_unchanged_when_no_field_is_provided() {
    ArticleCommandService service = new ArticleCommandService(articleRepository);
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "user-id");

    Article updated = service.updateArticle(article, new UpdateArticleParam());

    verify(articleRepository).save(updated);
    assertThat(updated.getTitle()).isEqualTo("old title");
    assertThat(updated.getDescription()).isEqualTo("old desc");
    assertThat(updated.getBody()).isEqualTo("old body");
  }
}
