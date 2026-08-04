package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
  public void should_update_title_and_regenerate_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");

    article.update("new title", "", "");

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_description_without_changing_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    String originalSlug = article.getSlug();

    article.update("", "new desc", "");

    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getSlug(), is(originalSlug));
  }

  @Test
  public void should_update_body_without_changing_slug() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    String originalSlug = article.getSlug();

    article.update("", "", "new body");

    assertThat(article.getBody(), is("new body"));
    assertThat(article.getSlug(), is(originalSlug));
  }

  @Test
  public void should_leave_fields_untouched_for_blank_arguments() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    String originalSlug = article.getSlug();

    article.update("", "", "");

    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
    assertThat(article.getSlug(), is(originalSlug));
  }

  @Test
  public void should_regenerate_slug_only_when_title_changes() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    String originalSlug = article.getSlug();

    article.update("", "new desc", "new body");
    assertThat(article.getSlug(), is(originalSlug));

    article.update("another title", "", "");
    assertThat(article.getSlug(), is(not(originalSlug)));
    assertThat(article.getSlug(), is("another-title"));
  }
}
