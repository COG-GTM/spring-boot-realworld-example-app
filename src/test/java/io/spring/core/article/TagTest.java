package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_generate_unique_ids_for_different_tags() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertThat(tag1.getId().equals(tag2.getId()), is(false));
  }

  @Test
  public void should_have_equality_based_on_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertThat(tag1.equals(tag2), is(true));
  }

  @Test
  public void should_not_be_equal_with_different_names() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertThat(tag1.equals(tag2), is(false));
  }
}
