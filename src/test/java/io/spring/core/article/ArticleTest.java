package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
  public void should_update_title_and_change_slug_and_updatedAt() {
    Article article =
        new Article(
            "Original Title", "desc", "body", Arrays.asList("java"), "123", new DateTime());
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update("New Title", "", "");

    assertEquals("New Title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertTrue(
        article.getUpdatedAt().isEqual(originalUpdatedAt)
            || article.getUpdatedAt().isAfter(originalUpdatedAt));
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_update_only_description() {
    Article article =
        new Article("Title", "original desc", "body", Arrays.asList("java"), "123", new DateTime());
    String originalSlug = article.getSlug();

    article.update("", "new desc", "");

    assertEquals("Title", article.getTitle());
    assertEquals(originalSlug, article.getSlug());
    assertEquals("new desc", article.getDescription());
    assertEquals("body", article.getBody());
  }

  @Test
  public void should_update_only_body() {
    Article article =
        new Article("Title", "desc", "original body", Arrays.asList("java"), "123", new DateTime());
    String originalSlug = article.getSlug();

    article.update("", "", "new body");

    assertEquals("Title", article.getTitle());
    assertEquals(originalSlug, article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("new body", article.getBody());
  }

  @Test
  public void should_not_change_fields_when_update_with_empty_values() {
    Article article =
        new Article(
            "Title", "desc", "body", Arrays.asList("java"), "123", new DateTime());
    DateTime originalUpdatedAt = article.getUpdatedAt();
    String originalSlug = article.getSlug();

    article.update("", "", "");

    assertEquals("Title", article.getTitle());
    assertEquals(originalSlug, article.getSlug());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(originalUpdatedAt, article.getUpdatedAt());
  }

  @Test
  public void should_not_change_fields_when_update_with_null_values() {
    Article article =
        new Article(
            "Title", "desc", "body", Arrays.asList("java"), "123", new DateTime());
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update(null, null, null);

    assertEquals("Title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(originalUpdatedAt, article.getUpdatedAt());
  }

  @Test
  public void should_deduplicate_tags_in_constructor() {
    Article article =
        new Article(
            "Title", "desc", "body", Arrays.asList("java", "java", "spring", "spring"), "123");

    List<String> tagNames =
        article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    assertEquals(2, tagNames.size());
    assertTrue(tagNames.contains("java"));
    assertTrue(tagNames.contains("spring"));
  }
}
