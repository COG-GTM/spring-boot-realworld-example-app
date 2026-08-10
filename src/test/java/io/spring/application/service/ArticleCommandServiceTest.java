package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ArticleCommandServiceTest {

  private final ArticleRepository articleRepository = mock(ArticleRepository.class);
  private final ArticleCommandService articleCommandService =
      new ArticleCommandService(articleRepository);

  @Test
  void should_create_article_from_new_article_param() {
    User creator = new User("jake@jake.jake", "jake", "123", "bio", "image");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to train your dragon")
            .description("Ever wonder how?")
            .body("You have to believe")
            .tagList(Arrays.asList("reactjs", "angularjs"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTitle()).isEqualTo("How to train your dragon");
    assertThat(article.getDescription()).isEqualTo("Ever wonder how?");
    assertThat(article.getBody()).isEqualTo("You have to believe");
    assertThat(article.getSlug()).isEqualTo("how-to-train-your-dragon");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    assertThat(article.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("reactjs", "angularjs");
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_and_regenerate_slug() {
    Article article =
        new Article("old title", "old description", "old body", Arrays.asList("tag"), "user-id");
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new description");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated).isSameAs(article);
    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getBody()).isEqualTo("new body");
    assertThat(updated.getDescription()).isEqualTo("new description");
    assertThat(updated.getSlug()).isEqualTo("new-title");
    verify(articleRepository).save(article);
  }

  @Test
  void should_keep_original_values_when_update_param_is_empty() {
    Article article =
        new Article("old title", "old description", "old body", Arrays.asList("tag"), "user-id");
    UpdateArticleParam param = new UpdateArticleParam();

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(param.getTitle()).isEmpty();
    assertThat(param.getBody()).isEmpty();
    assertThat(param.getDescription()).isEmpty();
    assertThat(updated.getTitle()).isEqualTo("old title");
    assertThat(updated.getBody()).isEqualTo("old body");
    assertThat(updated.getDescription()).isEqualTo("old description");
    assertThat(updated.getSlug()).isEqualTo("old-title");
    verify(articleRepository).save(article);
  }
}
