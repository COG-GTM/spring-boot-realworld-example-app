package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    String tagName = "java";

    Tag tag = new Tag(tagName);

    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is(tagName));
  }

  @Test
  public void should_generate_unique_id_for_each_tag() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");

    assertThat(tag1.getId(), notNullValue());
    assertThat(tag2.getId(), notNullValue());
  }

  @Test
  public void should_have_equality_based_on_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    Tag tag3 = new Tag("spring");

    assertThat(tag1, is(tag2));
    assertThat(tag1.hashCode(), is(tag2.hashCode()));
    assertThat(tag1, not(tag3));
  }

  @Test
  public void should_allow_setting_name() {
    Tag tag = new Tag("java");
    tag.setName("kotlin");

    assertThat(tag.getName(), is("kotlin"));
  }
}
