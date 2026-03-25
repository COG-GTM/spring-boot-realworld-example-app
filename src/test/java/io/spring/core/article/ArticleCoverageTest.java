package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleCoverageTest {

  @Test
  public void should_create_article_with_all_fields() {
    String title = "Test Title";
    String description = "Test Description";
    String body = "Test Body";
    List<String> tagList = Arrays.asList("java", "spring");
    String userId = "user-id-1";

    Article article = new Article(title, description, body, tagList, userId);

    assertNotNull(article.getId());
    assertEquals("test-title", article.getSlug());
    assertEquals(title, article.getTitle());
    assertEquals(description, article.getDescription());
    assertEquals(body, article.getBody());
    assertEquals(userId, article.getUserId());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_create_article_with_custom_datetime() {
    DateTime time = new DateTime(2023, 1, 1, 0, 0);
    Article article = new Article("title", "desc", "body", Arrays.asList("tag1"), "user1", time);

    assertEquals(time, article.getCreatedAt());
    assertEquals(time, article.getUpdatedAt());
  }

  @Test
  public void should_update_article_title() {
    DateTime pastTime = new DateTime(2020, 1, 1, 0, 0);
    Article article =
        new Article("Old Title", "desc", "body", Arrays.asList("tag1"), "user1", pastTime);

    article.update("New Title", "", "");

    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertNotEquals(pastTime, article.getUpdatedAt());
  }

  @Test
  public void should_update_article_description() {
    Article article =
        new Article("title", "Old Desc", "body", Arrays.asList("tag1"), "user1");

    article.update("", "New Desc", "");

    assertEquals("New Desc", article.getDescription());
  }

  @Test
  public void should_update_article_body() {
    Article article =
        new Article("title", "desc", "Old Body", Arrays.asList("tag1"), "user1");

    article.update("", "", "New Body");

    assertEquals("New Body", article.getBody());
  }

  @Test
  public void should_not_update_article_with_empty_values() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag1"), "user1");
    String originalTitle = article.getTitle();
    String originalDesc = article.getDescription();
    String originalBody = article.getBody();

    article.update("", "", "");

    assertEquals(originalTitle, article.getTitle());
    assertEquals(originalDesc, article.getDescription());
    assertEquals(originalBody, article.getBody());
  }

  @Test
  public void should_not_update_article_with_null_values() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag1"), "user1");

    article.update(null, null, null);

    assertEquals("title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_generate_slug_correctly() {
    assertEquals("hello-world", Article.toSlug("Hello World"));
    assertEquals("hello-world", Article.toSlug("hello world"));
    assertEquals("test-article-title", Article.toSlug("Test Article Title"));
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "user1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_create_article_with_empty_tags() {
    Article article =
        new Article("title", "desc", "body", Collections.emptyList(), "user1");
    assertEquals(0, article.getTags().size());
  }

  @Test
  public void should_have_correct_equals_and_hashcode() {
    Article article1 =
        new Article("title", "desc", "body", Arrays.asList("tag1"), "user1");
    Article article2 =
        new Article("title", "desc", "body", Arrays.asList("tag1"), "user1");

    // Different IDs since UUID is random
    assertNotEquals(article1, article2);
    assertEquals(article1, article1);
    assertNotEquals(article1.hashCode(), article2.hashCode());
  }

  @Test
  public void should_update_all_fields_at_once() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag1"), "user1");

    article.update("New Title", "New Desc", "New Body");

    assertEquals("New Title", article.getTitle());
    assertEquals("New Desc", article.getDescription());
    assertEquals("New Body", article.getBody());
    assertEquals("new-title", article.getSlug());
  }
}
