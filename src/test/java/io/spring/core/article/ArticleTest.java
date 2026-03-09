package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArticleTest {

  @Test
  void shouldCreateArticleWithCorrectFields() {
    String title = "How to Train Your Dragon";
    String description = "Ever wonder how?";
    String body = "You have to believe";
    List<String> tagList = Arrays.asList("reactjs", "angularjs", "dragons");
    String userId = "user-123";

    Article article = new Article(title, description, body, tagList, userId);

    assertEquals(title, article.getTitle());
    assertEquals(description, article.getDescription());
    assertEquals(body, article.getBody());
    assertEquals(userId, article.getUserId());
    assertNotNull(article.getId());
    assertNotNull(article.getSlug());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(article.getCreatedAt(), article.getUpdatedAt());
    assertEquals(3, article.getTags().size());
  }

  @Test
  void shouldGenerateSlugFromTitle() {
    assertEquals("how-to-train-your-dragon", Article.toSlug("How to Train Your Dragon"));
    assertEquals("hello-world", Article.toSlug("Hello World"));
    assertEquals("test-title", Article.toSlug("Test Title"));
  }

  @Test
  void shouldGetRightSlugWithMultipleSpaces() {
    Article article = new Article("a new   title", "desc", "body", Arrays.asList("java"), "123");
    assertEquals("a-new-title", article.getSlug());
  }

  @Test
  void shouldGetRightSlugWithNumberInTitle() {
    Article article = new Article("a new title 2", "desc", "body", Arrays.asList("java"), "123");
    assertEquals("a-new-title-2", article.getSlug());
  }

  @Test
  void shouldGetLowerCaseSlug() {
    Article article = new Article("A NEW TITLE", "desc", "body", Arrays.asList("java"), "123");
    assertEquals("a-new-title", article.getSlug());
  }

  @Test
  void shouldHandleOtherLanguageInSlug() {
    Article article = new Article("中文：标题", "desc", "body", Arrays.asList("java"), "123");
    assertEquals("中文-标题", article.getSlug());
  }

  @Test
  void shouldHandleSpecialCharactersInSlug() {
    assertEquals("what-the-hell-w", Article.toSlug("what?the.hell,w"));
    assertEquals("hello-world", Article.toSlug("Hello & World"));
    assertEquals("it's-a-test", Article.toSlug("It's a test"));
  }

  @Test
  void shouldUpdateArticleFields() {
    Article article =
        new Article("Old Title", "Old Description", "Old Body", Arrays.asList("tag1"), "user-1");

    assertEquals("old-title", article.getSlug());

    article.update("New Title", "New Description", "New Body");

    assertEquals("New Title", article.getTitle());
    assertEquals("New Description", article.getDescription());
    assertEquals("New Body", article.getBody());
    assertEquals("new-title", article.getSlug());
  }

  @Test
  void shouldNotUpdateFieldsWhenEmptyStringProvided() {
    Article article =
        new Article(
            "Original Title",
            "Original Description",
            "Original Body",
            Arrays.asList("tag1"),
            "user-1");

    article.update("", "", "");

    assertEquals("Original Title", article.getTitle());
    assertEquals("Original Description", article.getDescription());
    assertEquals("Original Body", article.getBody());
    assertEquals("original-title", article.getSlug());
  }

  @Test
  void shouldNotUpdateFieldsWhenNullProvided() {
    Article article =
        new Article(
            "Original Title",
            "Original Description",
            "Original Body",
            Arrays.asList("tag1"),
            "user-1");

    article.update(null, null, null);

    assertEquals("Original Title", article.getTitle());
    assertEquals("Original Description", article.getDescription());
    assertEquals("Original Body", article.getBody());
    assertEquals("original-title", article.getSlug());
  }

  @Test
  void shouldOnlyUpdateNonEmptyFields() {
    Article article =
        new Article(
            "Original Title",
            "Original Description",
            "Original Body",
            Arrays.asList("tag1"),
            "user-1");

    article.update("New Title", "", null);

    assertEquals("New Title", article.getTitle());
    assertEquals("Original Description", article.getDescription());
    assertEquals("Original Body", article.getBody());
    assertEquals("new-title", article.getSlug());
  }

  @Test
  void shouldDeduplicateTags() {
    Article article =
        new Article(
            "Title", "Desc", "Body", Arrays.asList("java", "java", "spring"), "user-1");

    assertEquals(2, article.getTags().size());
  }
}
