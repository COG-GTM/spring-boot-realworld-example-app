package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_set_name_and_generate_id() {
    Tag tag = new Tag("java");
    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_equal_when_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertThat(tag1.equals(tag2), is(true));
    assertThat(tag1.hashCode(), is(tag2.hashCode()));
  }

  @Test
  public void should_not_equal_when_different_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("python");
    assertThat(tag1.equals(tag2), is(false));
  }
}
