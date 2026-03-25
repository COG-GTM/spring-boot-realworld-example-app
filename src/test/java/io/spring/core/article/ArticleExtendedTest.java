package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleExtendedTest {

  @Test
  public void should_create_article_with_all_fields() {
    Article article =
        new Article("Test Title", "description", "body content", Arrays.asList("java", "spring"), "user1");
    assertNotNull(article.getId());
    assertEquals("test-title", article.getSlug());
    assertEquals("Test Title", article.getTitle());
    assertEquals("description", article.getDescription());
    assertEquals("body content", article.getBody());
    assertEquals("user1", article.getUserId());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_create_article_with_custom_created_at() {
    DateTime createdAt = new DateTime(2023, 1, 1, 0, 0);
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), "user1", createdAt);
    assertEquals(createdAt, article.getCreatedAt());
    assertEquals(createdAt, article.getUpdatedAt());
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1");
    assertTrue(article.getTags().isEmpty());
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java", "java", "spring"), "user1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_generate_unique_ids() {
    Article a1 = new Article("T1", "d1", "b1", Collections.emptyList(), "u1");
    Article a2 = new Article("T2", "d2", "b2", Collections.emptyList(), "u2");
    assertNotEquals(a1.getId(), a2.getId());
  }

  @Test
  public void should_update_title() {
    Article article = new Article("Old Title", "desc", "body", Collections.emptyList(), "user1");
    article.update("New Title", "", "");
    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_update_description() {
    Article article = new Article("Title", "old desc", "body", Collections.emptyList(), "user1");
    article.update("", "new desc", "");
    assertEquals("new desc", article.getDescription());
    assertEquals("Title", article.getTitle());
  }

  @Test
  public void should_update_body() {
    Article article = new Article("Title", "desc", "old body", Collections.emptyList(), "user1");
    article.update("", "", "new body");
    assertEquals("new body", article.getBody());
    assertEquals("Title", article.getTitle());
  }

  @Test
  public void should_update_multiple_fields() {
    Article article = new Article("Old", "old desc", "old body", Collections.emptyList(), "user1");
    article.update("New", "new desc", "new body");
    assertEquals("New", article.getTitle());
    assertEquals("new desc", article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  public void should_not_update_with_null_values() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1");
    article.update(null, null, null);
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_not_update_with_empty_values() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1");
    article.update("", "", "");
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_equal_by_id() {
    Article a1 = new Article("T1", "d1", "b1", Collections.emptyList(), "u1");
    Article a2 = new Article("T2", "d2", "b2", Collections.emptyList(), "u2");
    assertNotEquals(a1, a2);
    assertEquals(a1, a1);
  }

  @Test
  public void should_have_consistent_hashcode() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1");
    assertEquals(article.hashCode(), article.hashCode());
  }

  @Test
  public void should_update_updatedAt_on_title_change() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user1", createdAt);
    DateTime beforeUpdate = article.getUpdatedAt();
    article.update("New Title", "", "");
    assertNotEquals(beforeUpdate, article.getUpdatedAt());
  }

  @Test
  public void should_update_slug_on_title_change() {
    Article article = new Article("Original Title", "desc", "body", Collections.emptyList(), "user1");
    assertEquals("original-title", article.getSlug());
    article.update("Updated Title", "", "");
    assertEquals("updated-title", article.getSlug());
  }

  @Test
  public void should_handle_single_tag() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "user1");
    assertEquals(1, article.getTags().size());
    assertEquals("java", article.getTags().get(0).getName());
  }
}
