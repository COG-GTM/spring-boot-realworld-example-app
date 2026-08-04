package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_and_assign_name() {
    Tag tag = new Tag("java");

    assertNotNull(tag.getId());
    assertThat(tag.getName(), is("java"));
  }

  @Test
  public void should_generate_distinct_ids() {
    assertThat(new Tag("java").getId(), is(not(new Tag("java").getId())));
  }

  @Test
  public void should_compare_by_name_only_ignoring_id() {
    Tag a = new Tag("java");
    Tag b = new Tag("java");

    assertThat(a.getId(), is(not(b.getId())));
    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(new Tag("kotlin"))));
  }
}
