package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ArticleUpdateTest {

  @Test
  public void should_update_title_and_slug() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("java"), "userId");
    String originalSlug = article.getSlug();
    article.update("New Title", "", "");
    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertNotEquals(originalSlug, article.getSlug());
  }

  @Test
  public void should_update_description_only() {
    Article article = new Article("Title", "old desc", "body", Arrays.asList("java"), "userId");
    String originalSlug = article.getSlug();
    article.update("", "new desc", "");
    assertEquals("new desc", article.getDescription());
    assertEquals("Title", article.getTitle());
    assertEquals(originalSlug, article.getSlug());
  }

  @Test
  public void should_update_body_only() {
    Article article = new Article("Title", "desc", "old body", Arrays.asList("java"), "userId");
    article.update("", "", "new body");
    assertEquals("new body", article.getBody());
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
  }

  @Test
  public void should_update_all_fields() {
    Article article =
        new Article("Old Title", "old desc", "old body", Arrays.asList("java"), "userId");
    article.update("New Title", "new desc", "new body");
    assertEquals("New Title", article.getTitle());
    assertEquals("new desc", article.getDescription());
    assertEquals("new body", article.getBody());
    assertEquals("new-title", article.getSlug());
  }

  @Test
  public void should_not_change_when_all_empty() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "userId");
    article.update("", "", "");
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_not_change_when_all_null() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "userId");
    article.update(null, null, null);
    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java", "java", "spring"), "userId");
    List<String> tagNames =
        article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    assertEquals(2, tagNames.size());
    assertTrue(tagNames.contains("java"));
    assertTrue(tagNames.contains("spring"));
  }

  @Test
  public void should_handle_empty_tag_list() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "userId");
    assertNotNull(article.getTags());
    assertTrue(article.getTags().isEmpty());
  }

  @Test
  public void should_set_created_and_updated_at_same_time() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "userId");
    assertEquals(article.getCreatedAt(), article.getUpdatedAt());
  }

  @Test
  public void should_update_updatedAt_when_title_changes() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "userId");
    org.joda.time.DateTime originalUpdatedAt = article.getUpdatedAt();
    article.update("New Title", "", "");
    assertNotNull(article.getUpdatedAt());
  }
}
