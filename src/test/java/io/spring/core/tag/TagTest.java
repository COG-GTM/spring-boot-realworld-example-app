package io.spring.core.tag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Tag;
import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    String name = "java";

    Tag tag = new Tag(name);

    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is(name));
  }

  @Test
  public void should_generate_unique_id_for_each_tag() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");

    assertThat(tag1.getId(), not(tag2.getId()));
  }

  @Test
  public void should_have_equal_tags_with_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");

    assertThat(tag1.equals(tag2), is(true));
    assertThat(tag1.hashCode(), is(tag2.hashCode()));
  }

  @Test
  public void should_have_different_tags_with_different_names() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");

    assertThat(tag1.equals(tag2), is(false));
  }

  @Test
  public void should_handle_lowercase_tag_name() {
    Tag tag = new Tag("javascript");

    assertThat(tag.getName(), is("javascript"));
  }

  @Test
  public void should_handle_uppercase_tag_name() {
    Tag tag = new Tag("JAVA");

    assertThat(tag.getName(), is("JAVA"));
  }

  @Test
  public void should_handle_mixed_case_tag_name() {
    Tag tag = new Tag("JavaScript");

    assertThat(tag.getName(), is("JavaScript"));
  }

  @Test
  public void should_handle_tag_name_with_numbers() {
    Tag tag = new Tag("java11");

    assertThat(tag.getName(), is("java11"));
  }

  @Test
  public void should_handle_tag_name_with_hyphen() {
    Tag tag = new Tag("spring-boot");

    assertThat(tag.getName(), is("spring-boot"));
  }

  @Test
  public void should_handle_tag_name_with_underscore() {
    Tag tag = new Tag("spring_framework");

    assertThat(tag.getName(), is("spring_framework"));
  }

  @Test
  public void should_handle_empty_tag_name() {
    Tag tag = new Tag("");

    assertThat(tag.getName(), is(""));
  }

  @Test
  public void should_handle_single_character_tag_name() {
    Tag tag = new Tag("a");

    assertThat(tag.getName(), is("a"));
  }

  @Test
  public void should_handle_long_tag_name() {
    String longName = "this-is-a-very-long-tag-name-that-might-be-used-in-some-edge-cases";

    Tag tag = new Tag(longName);

    assertThat(tag.getName(), is(longName));
  }

  @Test
  public void should_not_be_equal_to_null() {
    Tag tag = new Tag("java");

    assertThat(tag.equals(null), is(false));
  }

  @Test
  public void should_be_equal_to_itself() {
    Tag tag = new Tag("java");

    assertThat(tag.equals(tag), is(true));
  }

  @Test
  public void should_have_consistent_hashcode() {
    Tag tag = new Tag("java");
    int hashCode1 = tag.hashCode();
    int hashCode2 = tag.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }

  @Test
  public void should_have_same_hashcode_for_equal_tags() {
    Tag tag1 = new Tag("spring");
    Tag tag2 = new Tag("spring");

    assertThat(tag1.hashCode(), is(tag2.hashCode()));
  }
}
