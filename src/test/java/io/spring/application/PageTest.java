package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_have_default_offset_and_limit() {
    Page page = new Page();
    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_set_valid_offset_and_limit() {
    Page page = new Page(5, 50);
    assertThat(page.getOffset(), is(5));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    Page page = new Page(0, 200);
    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_default_offset_to_zero_when_negative() {
    Page page = new Page(-1, 20);
    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_keep_default_limit_when_negative() {
    Page page = new Page(0, -1);
    assertThat(page.getLimit(), is(20));
  }
}
