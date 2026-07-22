package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_and_keep_name() {
    Tag tag = new Tag("java");
    assertThat(tag.getId(), is(notNullValue()));
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_be_equal_when_names_are_equal_ignoring_id() {
    Tag one = new Tag("java");
    Tag two = new Tag("java");
    Tag different = new Tag("spring");
    assertThat(one.equals(two), is(true));
    assertThat(one.hashCode(), is(two.hashCode()));
    assertThat(one.equals(different), is(false));
  }
}
