package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_for_new_tag() {
    Tag tag = new Tag("java");
    assertThat(tag.getId(), notNullValue());
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_be_equal_by_name_only() {
    assertThat(new Tag("java"), is(new Tag("java")));
    assertThat(new Tag("java").hashCode(), is(new Tag("java").hashCode()));
    assertThat(new Tag("java"), not(new Tag("spring")));
  }
}
