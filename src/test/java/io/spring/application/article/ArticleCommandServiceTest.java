package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ArticleCommandServiceTest {

  private ArticleRepository articleRepository;
  private ArticleCommandService articleCommandService;
  private User creator;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    articleCommandService = new ArticleCommandService(articleRepository);
    creator = new User("john@example.com", "john", "123", "bio", "image");
  }

  @Test
  public void should_create_article_from_param_and_persist_it() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to learn Spring Boot")
            .description("a guide")
            .body("the body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTitle()).isEqualTo("How to learn Spring Boot");
    assertThat(article.getDescription()).isEqualTo("a guide");
    assertThat(article.getBody()).isEqualTo("the body");
    assertThat(article.getUserId()).isEqualTo(creator.getId());
    assertThat(article.getSlug()).isEqualTo("how-to-learn-spring-boot");
    assertThat(article.getTags()).extracting("name").containsExactlyInAnyOrder("java", "spring");

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(captor.capture());
    assertThat(captor.getValue()).isSameAs(article);
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("No tags here")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, creator);

    assertThat(article.getTags()).isEmpty();
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  public void should_update_article_fields_and_persist_it() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("t"), creator.getId());
    String originalSlug = article.getSlug();

    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated).isSameAs(article);
    assertThat(updated.getTitle()).isEqualTo("new title");
    assertThat(updated.getBody()).isEqualTo("new body");
    assertThat(updated.getDescription()).isEqualTo("new desc");
    assertThat(updated.getSlug()).isEqualTo("new-title");
    assertThat(updated.getSlug()).isNotEqualTo(originalSlug);

    verify(articleRepository).save(article);
  }

  @Test
  public void should_keep_existing_values_when_update_param_is_empty() {
    Article article =
        new Article("keep title", "keep desc", "keep body", Arrays.asList("t"), creator.getId());

    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    Article updated = articleCommandService.updateArticle(article, param);

    assertThat(updated.getTitle()).isEqualTo("keep title");
    assertThat(updated.getBody()).isEqualTo("keep body");
    assertThat(updated.getDescription()).isEqualTo("keep desc");

    verify(articleRepository).save(article);
  }
}
