package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_and_keep_name_from_constructor() {
    Tag tag = new Tag("java");

    assertThat(tag.getId()).isNotNull();
    assertThat(UUID.fromString(tag.getId()).toString()).isEqualTo(tag.getId());
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  public void should_generate_different_id_for_each_tag() {
    assertThat(new Tag("java").getId()).isNotEqualTo(new Tag("java").getId());
  }

  @Test
  public void should_leave_all_fields_null_with_no_args_constructor() {
    Tag tag = new Tag();

    assertThat(tag.getId()).isNull();
    assertThat(tag.getName()).isNull();
  }

  @Test
  public void should_set_fields_with_setters() {
    Tag tag = new Tag();

    tag.setId("id");
    tag.setName("java");

    assertThat(tag.getId()).isEqualTo("id");
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  public void should_use_only_name_for_equals_and_hashcode() {
    Tag tag = new Tag("java");
    Tag sameNameDifferentId = new Tag("java");

    assertThat(sameNameDifferentId.getId()).isNotEqualTo(tag.getId());
    assertThat(sameNameDifferentId).isEqualTo(tag);
    assertThat(sameNameDifferentId.hashCode()).isEqualTo(tag.hashCode());
  }

  @Test
  public void should_not_be_equal_when_names_are_different() {
    assertThat(new Tag("java")).isNotEqualTo(new Tag("spring"));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    Tag tag = new Tag("java");

    assertThat(tag).isNotEqualTo(null);
    assertThat(tag).isNotEqualTo("java");
    assertThat(tag).isEqualTo(tag);
  }

  @Test
  public void should_be_equal_when_both_names_are_null() {
    assertThat(new Tag()).isEqualTo(new Tag());
    assertThat(new Tag().hashCode()).isEqualTo(new Tag().hashCode());
  }

  @Test
  public void should_expose_id_and_name_in_to_string() {
    Tag tag = new Tag("java");

    assertThat(tag.toString()).contains(tag.getId()).contains("java");
  }
}
