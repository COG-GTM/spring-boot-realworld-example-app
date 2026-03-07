package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
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
  public void should_create_article_with_all_fields() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java", "spring"), "123");

    assertThat(article.getId(), notNullValue());
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUserId(), is("123"));
    assertThat(article.getTags().size(), is(2));
    assertThat(article.getCreatedAt(), notNullValue());
    assertThat(article.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_ids() {
    Article a1 = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    Article a2 = new Article("title", "desc", "body", Arrays.asList("java"), "123");

    assertThat(a1.getId(), not(a2.getId()));
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");

    assertThat(article.getTags().size(), is(2));
  }

  @Test
  public void should_create_article_with_empty_tags() {
    Article article = new Article("title", "desc", "body", Collections.emptyList(), "123");

    assertThat(article.getTags().size(), is(0));
  }

  @Test
  public void should_update_title_and_slug() {
    Article article = new Article("Old Title", "desc", "body", Arrays.asList("java"), "123");
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update("New Title", "", "");

    assertThat(article.getTitle(), is("New Title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_description() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "123");

    article.update("", "new desc", "");

    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_body() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "123");

    article.update("", "", "new body");

    assertThat(article.getBody(), is("new body"));
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
  }

  @Test
  public void should_not_update_when_all_empty() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update("", "", "");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt(), is(originalUpdatedAt));
  }

  @Test
  public void should_update_all_fields_at_once() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");

    article.update("new title", "new desc", "new body");

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
    assertThat(article.getSlug(), is("new-title"));
  }

  @Test
  public void should_preserve_created_at_on_update() {
    DateTime createdAt = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", createdAt);

    article.update("new title", "", "");

    assertThat(article.getCreatedAt(), is(createdAt));
  }

  @Test
  public void should_have_equality_based_on_id() {
    Article a1 = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    Article a2 = new Article("title", "desc", "body", Arrays.asList("java"), "123");

    assertThat(a1.equals(a2), is(false));
    assertThat(a1.equals(a1), is(true));
  }
}
