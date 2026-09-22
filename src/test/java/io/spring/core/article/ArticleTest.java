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
  public void should_not_have_trailing_hyphen() {
    assertThat(Article.toSlug("How to train your dragon?"), is("how-to-train-your-dragon"));
  }

  @Test
  public void should_strip_punctuation() {
    assertThat(Article.toSlug("Hello, World!"), is("hello-world"));
  }

  @Test
  public void should_collapse_symbols_and_trim_whitespace() {
    assertThat(Article.toSlug("  C++  &  Java: 101  "), is("c-java-101"));
  }

  @Test
  public void should_keep_plain_title_behavior() {
    assertThat(Article.toSlug("a new title 2"), is("a-new-title-2"));
  }
}
