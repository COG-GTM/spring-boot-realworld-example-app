package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_generated_id() {
    Tag tag = new Tag("java");

    assertThat(tag.getId()).isNotEmpty();
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  public void should_compare_tags_by_name_only() {
    Tag a = new Tag("java");
    Tag b = new Tag("java");

    assertThat(a.getId()).isNotEqualTo(b.getId());
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(new Tag("spring"));
  }
}
