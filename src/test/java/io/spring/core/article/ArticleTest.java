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
  public void should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");
    assertThat(article.getTags().size(), is(2));
  }

  @Test
  public void should_update_slug_when_title_is_updated() {
    Article article = new Article("old title", "desc", "body", Arrays.asList("java"), "123");
    article.update("new title", "", "");

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
  }

  @Test
  public void should_keep_original_values_when_updating_with_empty_values() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    article.update("", null, "");

    assertThat(article.getTitle(), is("title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }
}
