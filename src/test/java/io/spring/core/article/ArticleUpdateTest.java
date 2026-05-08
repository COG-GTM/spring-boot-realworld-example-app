package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ArticleUpdateTest {

  @Test
  public void should_update_title_and_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "user1");
    String oldSlug = article.getSlug();
    article.update("new title", "", "");
    assertEquals("new title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertNotEquals(oldSlug, article.getSlug());
  }

  @Test
  public void should_update_description() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "user1");
    article.update("", "new desc", "");
    assertEquals("new desc", article.getDescription());
    assertEquals("title", article.getTitle());
  }

  @Test
  public void should_update_body() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "user1");
    article.update("", "", "new body");
    assertEquals("new body", article.getBody());
    assertEquals("title", article.getTitle());
  }

  @Test
  public void should_not_update_when_all_empty() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user1");
    String originalTitle = article.getTitle();
    String originalDesc = article.getDescription();
    String originalBody = article.getBody();
    article.update("", "", "");
    assertEquals(originalTitle, article.getTitle());
    assertEquals(originalDesc, article.getDescription());
    assertEquals(originalBody, article.getBody());
  }

  @Test
  public void should_not_update_when_all_null() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user1");
    article.update(null, null, null);
    assertEquals("title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_update_multiple_fields() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user1");
    article.update("new title", "new desc", "new body");
    assertEquals("new title", article.getTitle());
    assertEquals("new desc", article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  public void should_create_article_with_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "spring"), "user1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "user1");
    assertEquals(2, article.getTags().size());
  }

  @Test
  public void should_create_article_with_empty_tags() {
    Article article = new Article("title", "desc", "body", Collections.emptyList(), "user1");
    assertTrue(article.getTags().isEmpty());
  }

  @Test
  public void should_set_created_and_updated_at() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user1");
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(article.getCreatedAt(), article.getUpdatedAt());
  }

  @Test
  public void should_generate_unique_ids() {
    Article a1 = new Article("title1", "desc", "body", Arrays.asList("java"), "user1");
    Article a2 = new Article("title2", "desc", "body", Arrays.asList("java"), "user1");
    assertNotEquals(a1.getId(), a2.getId());
  }

  @Test
  public void should_store_user_id() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user123");
    assertEquals("user123", article.getUserId());
  }
}
