package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
  public void should_deduplicate_tags_in_constructor() {
    Article article =
        new Article(
            "title", "desc", "body", Arrays.asList("java", "spring", "java", "spring"), "user-1");

    assertEquals(2, article.getTags().size());
  }

  @Test
  public void update_should_change_title_slug_and_updatedAt() throws InterruptedException {
    DateTime initial = new DateTime(2020, 1, 1, 0, 0, 0, 0);
    Article article =
        new Article("old title", "desc", "body", Arrays.asList("java"), "user-1", initial);
    DateTime original = article.getUpdatedAt();
    Thread.sleep(5);

    article.update("new title", null, null);

    assertEquals("new title", article.getTitle());
    assertEquals("new-title", article.getSlug());
    assertNotEquals(original, article.getUpdatedAt());
    assertTrue(article.getUpdatedAt().isAfter(original));
  }

  @Test
  public void update_should_change_description_and_updatedAt() throws InterruptedException {
    DateTime initial = new DateTime(2020, 1, 1, 0, 0, 0, 0);
    Article article =
        new Article("title", "old desc", "body", Arrays.asList("java"), "user-1", initial);
    DateTime original = article.getUpdatedAt();
    Thread.sleep(5);

    article.update(null, "new desc", null);

    assertEquals("new desc", article.getDescription());
    assertTrue(article.getUpdatedAt().isAfter(original));
  }

  @Test
  public void update_should_change_body_and_updatedAt() throws InterruptedException {
    DateTime initial = new DateTime(2020, 1, 1, 0, 0, 0, 0);
    Article article =
        new Article("title", "desc", "old body", Arrays.asList("java"), "user-1", initial);
    DateTime original = article.getUpdatedAt();
    Thread.sleep(5);

    article.update(null, null, "new body");

    assertEquals("new body", article.getBody());
    assertTrue(article.getUpdatedAt().isAfter(original));
  }

  @Test
  public void update_should_ignore_null_and_empty_inputs() {
    DateTime initial = new DateTime(2020, 1, 1, 0, 0, 0, 0);
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), "user-1", initial);

    article.update(null, null, null);
    assertEquals("title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(initial, article.getUpdatedAt());

    article.update("", "", "");
    assertEquals("title", article.getTitle());
    assertEquals("desc", article.getDescription());
    assertEquals("body", article.getBody());
    assertEquals(initial, article.getUpdatedAt());
  }
}
