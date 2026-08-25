package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class ArticleDomainTest {

  @Test
  public void should_deduplicate_tags_and_set_timestamps_from_given_creation_time() {
    DateTime createdAt = new DateTime(1000L);

    Article article =
        new Article(
            "a title",
            "desc",
            "body",
            Arrays.asList("java", "java", "spring"),
            "userId",
            createdAt);

    assertThat(article.getTags())
        .extracting(Tag::getName)
        .containsExactlyInAnyOrder("java", "spring");
    assertThat(article.getCreatedAt()).isEqualTo(createdAt);
    assertThat(article.getUpdatedAt()).isEqualTo(createdAt);
    assertThat(article.getUserId()).isEqualTo("userId");
    assertThat(article.getId()).isNotBlank();
  }

  @Test
  public void should_not_change_anything_when_update_values_are_empty() {
    DateTime createdAt = new DateTime(1000L);
    Article article =
        new Article("a title", "desc", "body", Collections.emptyList(), "userId", createdAt);

    article.update("", null, "");

    assertThat(article.getTitle()).isEqualTo("a title");
    assertThat(article.getDescription()).isEqualTo("desc");
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getSlug()).isEqualTo("a-title");
    assertThat(article.getUpdatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void should_refresh_slug_and_updated_at_when_title_changes() {
    DateTime createdAt = new DateTime(1000L);
    Article article =
        new Article("a title", "desc", "body", Collections.emptyList(), "userId", createdAt);

    article.update("New Title", "", "");

    assertThat(article.getTitle()).isEqualTo("New Title");
    assertThat(article.getSlug()).isEqualTo("new-title");
    assertThat(article.getUpdatedAt()).isGreaterThan(createdAt);
  }

  @Test
  public void should_update_description_and_body_independently() {
    Article article =
        new Article(
            "a title", "desc", "body", Collections.emptyList(), "userId", new DateTime(1000L));

    article.update("", "new desc", "new body");

    assertThat(article.getDescription()).isEqualTo("new desc");
    assertThat(article.getBody()).isEqualTo("new body");
    assertThat(article.getTitle()).isEqualTo("a title");
  }

  @Test
  public void should_slugify_special_characters_and_whitespace() {
    assertThat(Article.toSlug("Hello World, Again?")).isEqualTo("hello-world-again-");
    assertThat(Article.toSlug("MiXeD CaSe")).isEqualTo("mixed-case");
  }

  @Test
  public void should_compare_articles_by_id_only() {
    Article article = new Article("a title", "desc", "body", Collections.emptyList(), "userId");
    Article another = new Article("a title", "desc", "body", Collections.emptyList(), "userId");

    assertThat(article).isEqualTo(article).hasSameHashCodeAs(article);
    assertThat(article).isNotEqualTo(another);
  }
}
