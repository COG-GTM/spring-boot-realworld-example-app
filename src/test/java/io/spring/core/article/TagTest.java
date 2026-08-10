package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagTest {

  @Test
  void should_create_tag_with_generated_id() {
    Tag tag = new Tag("java");

    assertThat(tag.getId()).isNotBlank();
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  void should_generate_unique_id_per_tag() {
    assertThat(new Tag("java").getId()).isNotEqualTo(new Tag("java").getId());
  }

  @Test
  void should_allow_mutating_fields() {
    Tag tag = new Tag();
    tag.setId("id");
    tag.setName("spring");

    assertThat(tag.getId()).isEqualTo("id");
    assertThat(tag.getName()).isEqualTo("spring");
  }

  @Test
  void should_compare_tags_by_name_only() {
    Tag tag = new Tag("java");
    Tag sameName = new Tag("java");
    Tag otherName = new Tag("spring");

    assertThat(tag).isEqualTo(tag).isEqualTo(sameName).hasSameHashCodeAs(sameName);
    assertThat(tag).isNotEqualTo(otherName);
    assertThat(tag).isNotEqualTo(null).isNotEqualTo("java");
    assertThat(tag).isNotEqualTo(new Tag());
    assertThat(tag.toString()).contains("java");
  }
}
