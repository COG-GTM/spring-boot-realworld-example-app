package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
  public void should_update_title_and_slug() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article =
        new Article("old title", "desc", "body", Arrays.asList("java"), "123", createdAt);

    article.update("new title", "", "");

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt().isAfter(createdAt), is(true));
    assertThat(article.getCreatedAt(), is(createdAt));
  }

  @Test
  public void should_update_description_and_body() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", createdAt);

    article.update(null, "new desc", "new body");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getSlug(), is("title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
    assertThat(article.getUpdatedAt().isAfter(createdAt), is(true));
  }

  @Test
  public void should_not_change_anything_on_empty_update() {
    DateTime createdAt = new DateTime().minusDays(1);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", createdAt);

    article.update("", null, "");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUpdatedAt(), is(createdAt));
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");

    assertThat(article.getTags().size(), is(2));
    assertThat(article.getTags().contains(new Tag("java")), is(true));
    assertThat(article.getTags().contains(new Tag("spring")), is(true));
  }

  @Test
  public void should_compare_articles_by_id() {
    Article article1 = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    Article article2 = new Article("title", "desc", "body", Arrays.asList("java"), "123");

    assertThat(article1, is(article1));
    assertThat(article1, not(article2));
  }

  @Test
  public void should_compare_tags_by_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");

    assertThat(tag1.getId(), not(tag2.getId()));
    assertThat(tag1, is(tag2));
    assertThat(tag1, not(new Tag("spring")));
  }
}
