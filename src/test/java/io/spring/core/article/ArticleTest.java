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
  public void should_soft_delete_and_restore_article() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.isDeleted(), is(false));

    article.softDelete();
    assertThat(article.isDeleted(), is(true));
    assertThat(article.getDeletedAt() != null, is(true));

    article.restore();
    assertThat(article.isDeleted(), is(false));
    assertThat(article.getDeletedAt() == null, is(true));
  }

  @Test
  public void should_keep_original_deleted_at_when_soft_deleted_twice() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    article.softDelete();
    DateTime firstDeletedAt = article.getDeletedAt();

    article.softDelete();
    assertThat(article.getDeletedAt(), is(firstDeletedAt));
  }
}
