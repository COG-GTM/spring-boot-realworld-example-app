package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults_with_no_args_constructor() {
    Page page = new Page();
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_keep_valid_offset_and_limit() {
    Page page = new Page(5, 30);
    assertThat(page.getOffset(), is(5));
    assertThat(page.getLimit(), is(30));
  }

  @Test
  public void should_cap_limit_at_max() {
    Page page = new Page(0, 500);
    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_fallback_to_default_limit_when_non_positive() {
    Page page = new Page(0, 0);
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_fallback_to_default_offset_when_non_positive() {
    Page page = new Page(-5, 10);
    assertThat(page.getOffset(), is(0));
  }
}
