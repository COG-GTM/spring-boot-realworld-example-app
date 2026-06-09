package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
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
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    String oldSlug = article.getSlug();
    article.update("new title", "", "");
    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getSlug(), not(oldSlug));
    assertThat(article.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_not_update_when_empty_values() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    String originalTitle = article.getTitle();
    String originalDesc = article.getDescription();
    String originalBody = article.getBody();
    article.update("", "", "");
    assertThat(article.getTitle(), is(originalTitle));
    assertThat(article.getDescription(), is(originalDesc));
    assertThat(article.getBody(), is(originalBody));
  }

  @Test
  public void should_update_description_only() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "123");
    article.update("", "new desc", "");
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_body_only() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "123");
    article.update("", "", "new body");
    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_remove_duplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");
    assertThat(article.getTags().size(), is(2));
  }

  @Test
  public void should_create_article_with_correct_fields() {
    Article article =
        new Article("my title", "my desc", "my body", Arrays.asList("tag1"), "user-id");
    assertThat(article.getId(), notNullValue());
    assertThat(article.getId().length(), is(36));
    assertThat(article.getSlug(), is("my-title"));
    assertThat(article.getTitle(), is("my title"));
    assertThat(article.getDescription(), is("my desc"));
    assertThat(article.getBody(), is("my body"));
    assertThat(article.getUserId(), is("user-id"));
    assertThat(article.getCreatedAt(), notNullValue());
    assertThat(article.getUpdatedAt(), notNullValue());
  }
}
