package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_assign_uuid_id_and_name() {
    Tag tag = new Tag("java");

    assertThat(UUID.fromString(tag.getId())).isNotNull();
    assertThat(tag.getName()).isEqualTo("java");
  }

  @Test
  public void should_assign_different_ids_to_tags_with_the_same_name() {
    assertThat(new Tag("java").getId()).isNotEqualTo(new Tag("java").getId());
  }

  @Test
  public void should_use_name_only_for_equality() {
    Tag tag = new Tag("java");
    Tag sameNameDifferentId = new Tag("java");
    Tag differentName = new Tag("spring");

    assertThat(tag).isEqualTo(sameNameDifferentId);
    assertThat(tag.hashCode()).isEqualTo(sameNameDifferentId.hashCode());
    assertThat(tag).isNotEqualTo(differentName);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    Tag tag = new Tag("java");

    assertThat(tag).isNotEqualTo(null);
    assertThat(tag).isNotEqualTo("java");
  }

  @Test
  public void should_support_no_args_constructor_and_setters() {
    Tag tag = new Tag();

    assertThat(tag.getId()).isNull();
    assertThat(tag.getName()).isNull();

    tag.setId("tag-id");
    tag.setName("java");

    assertThat(tag.getId()).isEqualTo("tag-id");
    assertThat(tag.getName()).isEqualTo("java");
    assertThat(tag.toString()).contains("java");
  }
}
