package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
  public void should_update_title_and_regenerate_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    DateTime before = article.getUpdatedAt();
    article.update("New Title", "", "");
    assertThat(article.getTitle(), is("New Title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertTrue(!article.getUpdatedAt().isBefore(before));
  }

  @Test
  public void should_update_description_and_body_only() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    article.update(null, "new desc", "new body");
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getSlug(), is("title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_not_change_anything_for_empty_update() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    DateTime updatedAt = article.getUpdatedAt();
    article.update("", "", "");
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt(), is(updatedAt));
  }

  @Test
  public void should_use_provided_created_at() {
    DateTime createdAt = new DateTime(1500000000000L);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", createdAt);
    assertThat(article.getCreatedAt(), is(createdAt));
    assertThat(article.getUpdatedAt(), is(createdAt));
  }
}
