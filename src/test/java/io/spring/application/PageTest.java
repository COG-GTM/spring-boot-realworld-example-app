package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_default_offset_and_limit() {
    Page page = new Page();
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_use_given_offset_and_limit() {
    Page page = new Page(10, 50);
    assertThat(page.getOffset(), is(10));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_fallback_to_default_when_offset_and_limit_are_not_positive() {
    Page page = new Page(-1, 0);
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_cap_limit_to_max() {
    Page page = new Page(0, 1000);
    assertThat(page.getLimit(), is(100));
  }
}
