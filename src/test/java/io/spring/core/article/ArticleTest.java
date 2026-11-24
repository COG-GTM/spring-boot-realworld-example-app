package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
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
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");
    assertThat(
        article.getTags().stream().map(Tag::getName).sorted().collect(Collectors.toList()),
        is(Arrays.asList("java", "spring")));
  }

  @Test
  public void should_initialize_fields_on_creation() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), "user-id", createdAt);
    assertThat(article.getId() != null, is(true));
    assertThat(article.getUserId(), is("user-id"));
    assertThat(article.getCreatedAt(), is(createdAt));
    assertThat(article.getUpdatedAt(), is(createdAt));
  }

  @Test
  public void should_update_title_and_slug() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("old title", "desc", "body", Arrays.asList(), "123", createdAt);

    article.update("new title", null, null);

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt().isAfter(createdAt), is(true));
  }

  @Test
  public void should_update_description_and_body() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("title", "desc", "body", Arrays.asList(), "123", createdAt);

    article.update("", "new desc", "new body");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getSlug(), is("title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
    assertThat(article.getUpdatedAt().isAfter(createdAt), is(true));
  }

  @Test
  public void should_not_change_anything_when_update_values_are_empty() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("title", "desc", "body", Arrays.asList(), "123", createdAt);

    article.update(null, "", null);

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt(), is(createdAt));
  }
}
