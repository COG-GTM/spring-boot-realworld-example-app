package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_id() {
    Tag tag = new Tag("java");
    assertThat(tag.getId(), notNullValue());
  }

  @Test
  public void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_equal_when_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertThat(tag1, is(tag2));
  }

  @Test
  public void should_not_equal_when_different_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("python");
    assertThat(tag1, is(not(tag2)));
  }

  @Test
  public void should_have_same_hashcode_when_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertThat(tag1.hashCode(), is(tag2.hashCode()));
  }
}
