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
    Article article = new Article("title", "description", "body", Arrays.asList("java", "spring"), "userId");

    assertThat(article.getId(), notNullValue());
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("description"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getUserId(), is("userId"));
    assertThat(article.getSlug(), is("title"));
    assertThat(article.getTags().size(), is(2));
    assertThat(article.getCreatedAt(), notNullValue());
    assertThat(article.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_id_for_each_article() {
    Article article1 = new Article("title1", "desc1", "body1", Arrays.asList("java"), "user1");
    Article article2 = new Article("title2", "desc2", "body2", Arrays.asList("java"), "user2");

    assertThat(article1.getId(), not(article2.getId()));
  }

  @Test
  public void should_create_article_with_custom_created_at() {
    DateTime customDate = new DateTime(2020, 1, 1, 0, 0);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "userId", customDate);

    assertThat(article.getCreatedAt(), is(customDate));
    assertThat(article.getUpdatedAt(), is(customDate));
  }

  @Test
  public void should_update_title_and_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "userId");
    DateTime originalUpdatedAt = article.getUpdatedAt();

    article.update("new title", null, null);

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_description() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "userId");

    article.update(null, "new description", null);

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("new description"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_body() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "userId");

    article.update(null, null, "new body");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_update_all_fields_at_once() {
    Article article = new Article("old title", "old desc", "old body", Arrays.asList("java"), "userId");

    article.update("new title", "new desc", "new body");

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_not_update_fields_when_empty_string() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "userId");

    article.update("", "", "");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_not_update_fields_when_null() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "userId");

    article.update(null, null, null);

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_deduplicate_tags() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "userId");

    assertThat(article.getTags().size(), is(2));
  }

  @Test
  public void should_create_article_with_empty_tags() {
    Article article = new Article("title", "desc", "body", Collections.emptyList(), "userId");

    assertThat(article.getTags().size(), is(0));
  }

  @Test
  public void should_have_equal_articles_with_same_id() {
    Article article1 = new Article("title1", "desc1", "body1", Arrays.asList("java"), "user1");
    Article article2 = new Article("title2", "desc2", "body2", Arrays.asList("spring"), "user2");

    assertThat(article1.equals(article1), is(true));
    assertThat(article1.equals(article2), is(false));
  }
}
