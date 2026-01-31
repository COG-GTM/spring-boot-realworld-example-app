package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
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
  public void should_update_title_and_slug_when_title_not_empty() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    article.update("new title", "", "");
    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
  }

  @Test
  public void should_not_update_title_when_empty() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    article.update("", "", "");
    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getSlug(), is("old-title"));
  }

  @Test
  public void should_update_description_when_not_empty() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "123");
    article.update("", "new desc", "");
    assertThat(article.getDescription(), is("new desc"));
  }

  @Test
  public void should_not_update_description_when_empty() {
    Article article = new Article("title", "old desc", "body", Arrays.asList("java"), "123");
    article.update("", "", "");
    assertThat(article.getDescription(), is("old desc"));
  }

  @Test
  public void should_update_body_when_not_empty() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "123");
    article.update("", "", "new body");
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_not_update_body_when_empty() {
    Article article = new Article("title", "desc", "old body", Arrays.asList("java"), "123");
    article.update("", "", "");
    assertThat(article.getBody(), is("old body"));
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    Article article = new Article("old title", "old desc", "old body", Arrays.asList("java"), "123");
    article.update("new title", "new desc", "new body");
    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("new body"));
  }
}
