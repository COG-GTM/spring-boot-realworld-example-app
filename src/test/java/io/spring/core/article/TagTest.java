package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_on_construction() {
    Tag tag = new Tag("java");
    assertThat(tag.getId()).isNotNull();
    assertThat(tag.getId()).isNotEmpty();
  }

  @Test
  public void should_set_name_on_construction() {
    Tag tag = new Tag("java");
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  public void should_generate_distinct_ids_for_different_instances() {
    Tag first = new Tag("java");
    Tag second = new Tag("java");
    assertThat(first.getId()).isNotEqualTo(second.getId());
  }

  @Test
  public void should_be_equal_when_names_match_regardless_of_id() {
    Tag first = new Tag("java");
    Tag second = new Tag("java");
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_names_differ() {
    Tag first = new Tag("java");
    Tag second = new Tag("kotlin");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_allow_setting_fields_via_setters() {
    Tag tag = new Tag();
    tag.setId("id");
    tag.setName("scala");
    assertThat(tag.getId()).isEqualTo("id");
    assertThat(tag.getName()).isEqualTo("scala");
  }
}
