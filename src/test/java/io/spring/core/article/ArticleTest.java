package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleTest {

  @Test
  public void should_get_right_slug() {
    Article article = new Article("a new   title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title"));
  }

  @Test
  public void should_get_right_slug_with_number_in_title() {
    Article article = new Article("a new title 2", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title-2"));
  }

  @Test
  public void should_get_lower_case_slug() {
    Article article = new Article("A NEW TITLE", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title"));
  }

  @Test
  public void should_handle_other_language() {
    Article article = new Article("中文：标题", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("中文-标题"));
  }

  @Test
  public void should_handle_commas() {
    Article article = new Article("what?the.hell,w", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("what-the-hell-w"));
  }

  @Test
  public void should_create_article_with_all_fields() {
    Article article = new Article("Test Title", "description", "body content", Arrays.asList("java", "spring"), "user-123");

    assertNotNull(article.getId());
    assertEquals("test-title", article.getSlug());
    assertEquals("Test Title", article.getTitle());
    assertEquals("description", article.getDescription());
    assertEquals("body content", article.getBody());
    assertEquals("user-123", article.getUserId());
    assertEquals(2, article.getTags().size());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
  }

  @Test
  public void should_create_article_with_custom_created_at() {
    DateTime customTime = new DateTime(2023, 1, 15, 10, 30);
    Article article = new Article("Title", "desc", "body", Arrays.asList("tag"), "user-123", customTime);

    assertEquals(customTime, article.getCreatedAt());
    assertEquals(customTime, article.getUpdatedAt());
  }

  @Test
  public void should_generate_unique_id_for_each_article() {
    Article article1 = new Article("Title 1", "desc", "body", Arrays.asList("tag"), "user-123");
    Article article2 = new Article("Title 2", "desc", "body", Arrays.asList("tag"), "user-123");

    assertNotEquals(article1.getId(), article2.getId());
  }

  @Test
  public void should_deduplicate_tags() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java", "java", "spring"), "user-123");

    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_update_title_when_not_empty() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("tag"), "user-123");
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update("New Title", null, null);

    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertTrue(article.getUpdatedAt().isAfter(originalUpdatedAt) || article.getUpdatedAt().equals(originalUpdatedAt));
  }

  @Test
  public void should_not_update_title_when_empty() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("tag"), "user-123");

    article.update("", null, null);

    assertEquals("Old Title", article.getTitle());
    assertEquals("old-title", article.getSlug());
  }

  @Test
  public void should_not_update_title_when_null() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("tag"), "user-123");

    article.update(null, null, null);

    assertEquals("Old Title", article.getTitle());
    assertEquals("old-title", article.getSlug());
  }

  @Test
  public void should_update_description_when_not_empty() {
    Article article = new Article("Title", "old desc", "body", Arrays.asList("tag"), "user-123");

    article.update(null, "new desc", null);

    assertEquals("new desc", article.getDescription());
  }

  @Test
  public void should_not_update_description_when_empty() {
    Article article = new Article("Title", "old desc", "body", Arrays.asList("tag"), "user-123");

    article.update(null, "", null);

    assertEquals("old desc", article.getDescription());
  }

  @Test
  public void should_update_body_when_not_empty() {
    Article article = new Article("Title", "desc", "old body", Arrays.asList("tag"), "user-123");

    article.update(null, null, "new body");

    assertEquals("new body", article.getBody());
  }

  @Test
  public void should_not_update_body_when_empty() {
    Article article = new Article("Title", "desc", "old body", Arrays.asList("tag"), "user-123");

    article.update(null, null, "");

    assertEquals("old body", article.getBody());
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    Article article = new Article("Old Title", "old desc", "old body", Arrays.asList("tag"), "user-123");

    article.update("New Title", "new desc", "new body");

    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertEquals("new desc", article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  public void should_have_equals_based_on_id() {
    Article article1 = new Article("Title", "desc", "body", Arrays.asList("tag"), "user-123");
    Article article2 = new Article("Title", "desc", "body", Arrays.asList("tag"), "user-123");

    assertNotEquals(article1, article2);
    assertEquals(article1, article1);
  }

  @Test
  public void should_handle_empty_tag_list() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user-123");

    assertTrue(article.getTags().isEmpty());
  }
}
