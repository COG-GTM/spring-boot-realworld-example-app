package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArticleUpdateTest {

  @Test
  void update_title_should_update_slug_and_updatedAt() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("java"), "user1");
    var originalUpdatedAt = article.getUpdatedAt();
    article.update("New Title", "", "");
    assertEquals("new-title", article.getSlug());
    assertEquals("New Title", article.getTitle());
    assertTrue(
        article.getUpdatedAt().isEqual(originalUpdatedAt)
            || article.getUpdatedAt().isAfter(originalUpdatedAt));
  }

  @Test
  void update_only_description() {
    Article article = new Article("Title", "old desc", "body", Arrays.asList("java"), "user1");
    String originalTitle = article.getTitle();
    String originalBody = article.getBody();
    article.update("", "new desc", "");
    assertEquals(originalTitle, article.getTitle());
    assertEquals("new desc", article.getDescription());
    assertEquals(originalBody, article.getBody());
  }

  @Test
  void update_only_body() {
    Article article = new Article("Title", "desc", "old body", Arrays.asList("java"), "user1");
    String originalTitle = article.getTitle();
    String originalDesc = article.getDescription();
    article.update("", "", "new body");
    assertEquals(originalTitle, article.getTitle());
    assertEquals(originalDesc, article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  void update_with_empty_or_null_should_not_change_values() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "user1");
    String originalTitle = article.getTitle();
    String originalDesc = article.getDescription();
    String originalBody = article.getBody();
    article.update("", "", "");
    assertEquals(originalTitle, article.getTitle());
    assertEquals(originalDesc, article.getDescription());
    assertEquals(originalBody, article.getBody());
  }

  @Test
  void tags_should_be_deduplicated() {
    List<String> tagsWithDuplicates = Arrays.asList("java", "spring", "java", "spring", "kotlin");
    Article article = new Article("Title", "desc", "body", tagsWithDuplicates, "user1");
    assertEquals(3, article.getTags().size());
  }

  @Test
  void createdAt_and_updatedAt_should_be_set_on_construction() {
    Article article = new Article("Title", "desc", "body", Arrays.asList("java"), "user1");
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(article.getCreatedAt(), article.getUpdatedAt());
  }
}
