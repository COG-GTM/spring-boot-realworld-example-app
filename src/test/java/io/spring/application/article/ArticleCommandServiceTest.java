package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @Test
  public void should_create_article_and_save_it() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("How to Train Your Dragon")
            .description("Ever wonder how?")
            .body("You have to believe")
            .tagList(Arrays.asList("dragons", "training", "dragons"))
            .build();

    Article article = articleCommandService.createArticle(param, user);

    ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository).save(captor.capture());
    assertSame(article, captor.getValue());
    assertEquals("How to Train Your Dragon", article.getTitle());
    assertEquals("how-to-train-your-dragon", article.getSlug());
    assertEquals("Ever wonder how?", article.getDescription());
    assertEquals("You have to believe", article.getBody());
    assertEquals(user.getId(), article.getUserId());
    assertEquals(
        2, article.getTags().size(), "duplicated tags should be collapsed into a single tag");
    assertTrue(
        article.getTags().stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())
            .containsAll(Arrays.asList("dragons", "training")));
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Collections.emptyList())
            .build();

    Article article = articleCommandService.createArticle(param, user);

    assertTrue(article.getTags().isEmpty());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_update_all_article_fields_and_save() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article =
        new Article(
            "old title", "old desc", "old body", Arrays.asList("java"), user.getId(), createdAt);

    Article updated =
        articleCommandService.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new desc"));

    assertSame(article, updated);
    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new body", updated.getBody());
    assertEquals("new desc", updated.getDescription());
    assertTrue(updated.getUpdatedAt().isAfter(createdAt));
    verify(articleRepository).save(article);
  }

  @Test
  public void should_keep_existing_values_for_empty_update_fields() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article =
        new Article(
            "old title", "old desc", "old body", Arrays.asList("java"), user.getId(), createdAt);

    articleCommandService.updateArticle(article, new UpdateArticleParam("", "new body", ""));

    assertEquals("old title", article.getTitle());
    assertEquals("old-title", article.getSlug());
    assertEquals("old desc", article.getDescription());
    assertEquals("new body", article.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  public void should_not_touch_updated_at_when_nothing_changes() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), user.getId(), createdAt);

    articleCommandService.updateArticle(article, new UpdateArticleParam());

    assertEquals(createdAt, article.getUpdatedAt());
    assertNotEquals(null, article.getSlug());
    verify(articleRepository).save(article);
  }
}
