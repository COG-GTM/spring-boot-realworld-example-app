package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ArticleBehaviorTest {

  @Test
  public void should_generate_id_and_default_timestamps_on_construction() {
    Article article = new Article("a title", "desc", "body", Arrays.asList("java"), "userId");
    assertThat(article.getId()).isNotNull().isNotEmpty();
    assertThat(article.getCreatedAt()).isNotNull();
    assertThat(article.getUpdatedAt()).isEqualTo(article.getCreatedAt());
    assertThat(article.getUserId()).isEqualTo("userId");
  }

  @Test
  public void should_map_tag_list_to_tags() {
    Article article =
        new Article("a title", "desc", "body", Arrays.asList("java", "spring"), "userId");
    List<String> names = article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    assertThat(names).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  public void should_deduplicate_tags() {
    Article article =
        new Article("a title", "desc", "body", Arrays.asList("java", "java"), "userId");
    assertThat(article.getTags()).hasSize(1);
    assertThat(article.getTags().get(0).getName()).isEqualTo("java");
  }

  @Test
  public void should_have_empty_tags_when_tag_list_empty() {
    Article article = new Article("a title", "desc", "body", Collections.emptyList(), "userId");
    assertThat(article.getTags()).isEmpty();
  }

  @Test
  public void should_regenerate_slug_when_title_updated() {
    Article article = new Article("old title", "desc", "body", Collections.emptyList(), "userId");
    article.update("brand new title", "", "");
    assertThat(article.getTitle()).isEqualTo("brand new title");
    assertThat(article.getSlug()).isEqualTo("brand-new-title");
  }

  @Test
  public void should_not_change_title_or_slug_when_update_title_empty() {
    Article article = new Article("old title", "desc", "body", Collections.emptyList(), "userId");
    String originalSlug = article.getSlug();
    article.update("", "new desc", "new body");
    assertThat(article.getTitle()).isEqualTo("old title");
    assertThat(article.getSlug()).isEqualTo(originalSlug);
    assertThat(article.getDescription()).isEqualTo("new desc");
    assertThat(article.getBody()).isEqualTo("new body");
  }

  @Test
  public void should_not_change_fields_when_all_update_values_empty() {
    Article article =
        new Article("old title", "old desc", "old body", Collections.emptyList(), "userId");
    article.update("", "", "");
    assertThat(article.getTitle()).isEqualTo("old title");
    assertThat(article.getDescription()).isEqualTo("old desc");
    assertThat(article.getBody()).isEqualTo("old body");
  }

  @Test
  public void should_update_description_and_body_independently() {
    Article article =
        new Article("title", "old desc", "old body", Collections.emptyList(), "userId");
    article.update("", "new desc", "");
    assertThat(article.getDescription()).isEqualTo("new desc");
    assertThat(article.getBody()).isEqualTo("old body");
  }

  @Test
  public void should_be_equal_when_ids_match() {
    Article article = new Article("title", "desc", "body", Collections.emptyList(), "userId");
    Article same =
        new Article("other", "otherDesc", "otherBody", Collections.emptyList(), "otherUser");
    setId(same, article.getId());
    assertThat(article).isEqualTo(same);
    assertThat(article.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_differ() {
    Article first = new Article("title", "desc", "body", Collections.emptyList(), "userId");
    Article second = new Article("title", "desc", "body", Collections.emptyList(), "userId");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_generate_slug_via_static_helper() {
    assertThat(Article.toSlug("A New   Title")).isEqualTo("a-new-title");
    assertThat(Article.toSlug("what?the.hell,w")).isEqualTo("what-the-hell-w");
  }

  private void setId(Article article, String id) {
    try {
      java.lang.reflect.Field field = Article.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(article, id);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
