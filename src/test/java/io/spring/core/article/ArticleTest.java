package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  public void should_set_created_at_to_now_when_using_default_constructor() {
    Instant before = Instant.now();
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    Instant after = Instant.now();
    
    assertNotNull(article.getCreatedAt());
    assertTrue(article.getCreatedAt().compareTo(before) >= 0);
    assertTrue(article.getCreatedAt().compareTo(after) <= 0);
  }

  @Test
  public void should_set_updated_at_equal_to_created_at_on_creation() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    
    assertNotNull(article.getUpdatedAt());
    assertEquals(article.getCreatedAt(), article.getUpdatedAt());
  }

  @Test
  public void should_use_provided_created_at_when_specified() {
    Instant customTime = Instant.parse("2025-01-01T00:00:00Z");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", customTime);
    
    assertEquals(customTime, article.getCreatedAt());
    assertEquals(customTime, article.getUpdatedAt());
  }

  @Test
  public void should_update_updated_at_when_title_changes() {
    Instant originalTime = Instant.now().minus(1, ChronoUnit.HOURS);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", originalTime);
    
    Instant beforeUpdate = Instant.now();
    article.update("new title", null, null);
    Instant afterUpdate = Instant.now();
    
    assertTrue(article.getUpdatedAt().compareTo(beforeUpdate) >= 0);
    assertTrue(article.getUpdatedAt().compareTo(afterUpdate) <= 0);
    assertEquals(originalTime, article.getCreatedAt());
  }

  @Test
  public void should_update_updated_at_when_description_changes() {
    Instant originalTime = Instant.now().minus(1, ChronoUnit.HOURS);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", originalTime);
    
    Instant beforeUpdate = Instant.now();
    article.update(null, "new description", null);
    Instant afterUpdate = Instant.now();
    
    assertTrue(article.getUpdatedAt().compareTo(beforeUpdate) >= 0);
    assertTrue(article.getUpdatedAt().compareTo(afterUpdate) <= 0);
  }

  @Test
  public void should_update_updated_at_when_body_changes() {
    Instant originalTime = Instant.now().minus(1, ChronoUnit.HOURS);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", originalTime);
    
    Instant beforeUpdate = Instant.now();
    article.update(null, null, "new body");
    Instant afterUpdate = Instant.now();
    
    assertTrue(article.getUpdatedAt().compareTo(beforeUpdate) >= 0);
    assertTrue(article.getUpdatedAt().compareTo(afterUpdate) <= 0);
  }

  @Test
  public void should_preserve_instant_precision() {
    Instant preciseTime = Instant.ofEpochMilli(1734629525403L);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123", preciseTime);
    
    assertEquals(1734629525403L, article.getCreatedAt().toEpochMilli());
  }

  @Test
  public void should_have_created_at_as_instant_type() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getCreatedAt(), is(notNullValue()));
    assertTrue(article.getCreatedAt() instanceof Instant);
  }

  @Test
  public void should_have_updated_at_as_instant_type() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getUpdatedAt(), is(notNullValue()));
    assertTrue(article.getUpdatedAt() instanceof Instant);
  }
}
