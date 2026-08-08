package io.spring.core.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_and_keep_name() {
    Tag tag = new Tag("java");

    assertThat(tag.getName()).isEqualTo("java");
    assertThat(tag.getId()).isNotBlank();
  }

  @Test
  public void should_generate_different_ids_for_same_name() {
    assertThat(new Tag("java").getId()).isNotEqualTo(new Tag("java").getId());
  }

  @Test
  public void should_use_name_for_equality() {
    Tag one = new Tag("java");
    Tag two = new Tag("java");
    Tag other = new Tag("spring");

    assertThat(one).isEqualTo(two);
    assertThat(one.hashCode()).isEqualTo(two.hashCode());
    assertThat(one).isNotEqualTo(other);
  }

  @Test
  public void should_support_no_args_constructor_and_setters() {
    Tag tag = new Tag();
    assertThat(tag.getName()).isNull();
    assertThat(tag.getId()).isNull();

    tag.setId("id");
    tag.setName("name");

    assertThat(tag.getId()).isEqualTo("id");
    assertThat(tag.getName()).isEqualTo("name");
    assertThat(tag.toString()).contains("name");
  }
}
