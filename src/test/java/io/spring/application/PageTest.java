package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults_for_empty_page() {
    Page page = new Page();
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_keep_valid_offset_and_limit() {
    Page page = new Page(10, 50);
    assertThat(page.getOffset(), is(10));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_cap_limit_at_max() {
    assertThat(new Page(0, 1000).getLimit(), is(100));
  }

  @Test
  public void should_fall_back_to_defaults_for_non_positive_values() {
    Page page = new Page(-1, 0);
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }
}
