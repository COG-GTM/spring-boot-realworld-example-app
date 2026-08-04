package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults_for_no_args_constructor() {
    Page page = new Page();

    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_keep_offset_and_limit_within_range() {
    Page page = new Page(10, 50);

    assertThat(page.getOffset(), is(10));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_fall_back_to_default_offset_when_not_positive() {
    assertThat(new Page(0, 20).getOffset(), is(0));
    assertThat(new Page(-5, 20).getOffset(), is(0));
  }

  @Test
  public void should_fall_back_to_default_limit_when_not_positive() {
    assertThat(new Page(0, 0).getLimit(), is(20));
    assertThat(new Page(0, -1).getLimit(), is(20));
  }

  @Test
  public void should_clamp_limit_to_max() {
    assertThat(new Page(0, 101).getLimit(), is(100));
    assertThat(new Page(0, 1000).getLimit(), is(100));
    assertThat(new Page(0, 100).getLimit(), is(100));
  }

  @Test
  public void should_be_equal_for_same_offset_and_limit() {
    assertThat(new Page(3, 7), is(new Page(3, 7)));
    assertThat(new Page(3, 7).hashCode(), is(new Page(3, 7).hashCode()));
  }
}
