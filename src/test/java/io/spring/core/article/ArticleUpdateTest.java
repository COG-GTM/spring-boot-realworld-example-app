package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleUpdateTest {

  @Test
  void should_create_article_with_correct_slug() {
    Article article = new Article("Hello World", "desc", "body", Arrays.asList("java"), "userId1");
    assertEquals("hello-world", article.getSlug());
    assertEquals("Hello World", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals("userId1", article.getUserId());
    assertNotNull(article.getId());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
  }

  @Test
  void should_create_article_with_tags() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java", "spring"), "userId1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  void should_deduplicate_tags() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java", "java", "spring"), "userId1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  void should_create_article_with_empty_tags() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "userId1");
    assertEquals(0, article.getTags().size());
  }

  @Test
  void should_update_title_and_regenerate_slug() {
    Article article = new Article("Old Title", "desc", "body", Collections.emptyList(), "userId1");
    DateTime originalUpdatedAt = article.getUpdatedAt();
    article.update("New Title", null, null);
    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertTrue(
        article.getUpdatedAt().isEqual(originalUpdatedAt)
            || article.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void should_update_description_only() {
    Article article = new Article("Title", "old desc", "body", Collections.emptyList(), "userId1");
    article.update(null, "new desc", null);
    assertEquals("Title", article.getTitle());
    assertEquals("new desc", article.getDescription());
  }

  @Test
  void should_update_body_only() {
    Article article = new Article("Title", "desc", "old body", Collections.emptyList(), "userId1");
    article.update(null, null, "new body");
    assertEquals("new body", article.getBody());
    assertEquals("desc", article.getDescription());
  }

  @Test
  void should_update_all_fields() {
    Article article =
        new Article("Old Title", "old desc", "old body", Collections.emptyList(), "userId1");
    article.update("New Title", "new desc", "new body");
    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertEquals("new desc", article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  void should_not_update_when_empty_strings() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "userId1");
    article.update("", "", "");
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  void should_not_update_when_null_values() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "userId1");
    article.update(null, null, null);
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  void should_generate_slug_with_special_characters() {
    assertEquals("hello-world", Article.toSlug("Hello World"));
    assertEquals("hello-world", Article.toSlug("Hello  World"));
    assertEquals("hello-world", Article.toSlug("Hello, World"));
    assertEquals("hello-world", Article.toSlug("Hello. World"));
    assertEquals("hello-world", Article.toSlug("Hello? World"));
  }

  @Test
  void should_generate_slug_with_ampersand() {
    assertEquals("java-spring", Article.toSlug("Java & Spring"));
  }

  @Test
  void should_create_article_with_custom_datetime() {
    DateTime customTime = new DateTime(2020, 1, 1, 0, 0);
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), "userId1", customTime);
    assertEquals(customTime, article.getCreatedAt());
    assertEquals(customTime, article.getUpdatedAt());
  }

  @Test
  void should_have_equality_based_on_id() {
    Article article1 = new Article("Title1", "desc1", "body1", Collections.emptyList(), "userId1");
    Article article2 = new Article("Title2", "desc2", "body2", Collections.emptyList(), "userId2");
    assertNotEquals(article1, article2);
    assertEquals(article1, article1);
  }
}
