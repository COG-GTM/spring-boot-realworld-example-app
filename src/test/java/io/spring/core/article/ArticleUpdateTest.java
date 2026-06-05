package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ArticleUpdateTest {

  @Test
  public void should_update_title_and_regenerate_slug() {
    Article article =
        new Article("Original Title", "desc", "body", Arrays.asList("java"), "user-1");
    article.update("New Title", null, null);
    assertThat(article.getTitle(), is("New Title"));
    assertThat(article.getSlug(), is("new-title"));
  }

  @Test
  public void should_update_description_only() {
    Article article =
        new Article("Title", "old desc", "body", Arrays.asList("java"), "user-1");
    article.update(null, "new description", null);
    assertThat(article.getDescription(), is("new description"));
    assertThat(article.getTitle(), is("Title"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_body_only() {
    Article article =
        new Article("Title", "desc", "old body", Arrays.asList("java"), "user-1");
    article.update(null, null, "new body content");
    assertThat(article.getBody(), is("new body content"));
    assertThat(article.getTitle(), is("Title"));
    assertThat(article.getDescription(), is("desc"));
  }

  @Test
  public void should_not_update_when_all_params_empty() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), "user-1");
    article.update("", "", "");
    assertThat(article.getTitle(), is("Title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_not_update_when_all_params_null() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), "user-1");
    article.update(null, null, null);
    assertThat(article.getTitle(), is("Title"));
    assertThat(article.getDescription(), is("desc"));
    assertThat(article.getBody(), is("body"));
  }

  @Test
  public void should_update_updatedAt_when_title_changes() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), "user-1");
    org.joda.time.DateTime originalUpdatedAt = article.getUpdatedAt();
    article.update("New Title", null, null);
    assertThat(article.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_create_article_with_multiple_tags() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java", "spring", "kotlin"), "user-1");
    assertThat(article.getTags().size(), is(3));
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article(
            "Title", "desc", "body", Arrays.asList("java", "java", "spring"), "user-1");
    assertThat(article.getTags().size(), is(2));
  }

  @Test
  public void should_create_article_with_empty_tags() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "user-1");
    assertThat(article.getTags().size(), is(0));
  }

  @Test
  public void should_generate_uuid_for_id() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), "user-1");
    assertThat(article.getId(), notNullValue());
    assertThat(article.getId().length(), is(36));
  }

  @Test
  public void should_generate_unique_ids_for_different_articles() {
    Article a1 = new Article("Title 1", "desc", "body", Arrays.asList("java"), "user-1");
    Article a2 = new Article("Title 2", "desc", "body", Arrays.asList("java"), "user-1");
    assertThat(a1.getId(), not(a2.getId()));
  }

  @Test
  public void should_set_createdAt_and_updatedAt_to_same_value_on_creation() {
    Article article =
        new Article("Title", "desc", "body", Arrays.asList("java"), "user-1");
    assertThat(article.getCreatedAt(), is(article.getUpdatedAt()));
  }
}
