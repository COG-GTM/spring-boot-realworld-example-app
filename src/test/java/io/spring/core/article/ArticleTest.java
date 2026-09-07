package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
  public void should_dedupe_tags() {
    Article article =
        new Article("title", "description", "body", Arrays.asList("java", "java", "spring"), "123");

    assertThat(article.getTags().size(), is(2));
    assertThat(article.getTags().contains(new Tag("java")), is(true));
    assertThat(article.getTags().contains(new Tag("spring")), is(true));
  }

  @Test
  public void should_update_title_and_slug_and_updatedAt() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article =
        new Article("Old Title", "description", "body", Arrays.asList("java"), "123", createdAt);

    article.update("New Title", null, null);

    assertThat(article.getTitle(), is("New Title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("description"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt().isAfter(createdAt), is(true));
  }

  @Test
  public void should_ignore_empty_update_fields() {
    DateTime createdAt = new DateTime();
    Article article =
        new Article("title", "description", "body", Arrays.asList("java"), "123", createdAt);

    article.update("", null, "");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("description"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt(), is(createdAt));
  }

  @Test
  public void should_update_description_and_body_only() {
    Article article = new Article("title", "description", "body", Arrays.asList("java"), "123");

    article.update(null, "new description", "new body");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getSlug(), is("title"));
    assertThat(article.getDescription(), is("new description"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_be_equal_by_id() {
    Article first = new Article("title", "description", "body", Arrays.asList("java"), "123");
    Article second = new Article("title", "description", "body", Arrays.asList("java"), "123");

    assertThat(first.equals(second), is(false));
    assertThat(first.equals(first), is(true));
  }
}
