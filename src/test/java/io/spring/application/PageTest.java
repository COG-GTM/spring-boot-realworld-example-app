package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_default_values() {
    Page page = new Page(0, 0);

    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_set_valid_offset_and_limit() {
    Page page = new Page(10, 50);

    assertThat(page.getOffset(), is(10));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_cap_limit_at_max() {
    Page page = new Page(0, 200);

    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_ignore_negative_offset() {
    Page page = new Page(-5, 20);

    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_ignore_negative_limit() {
    Page page = new Page(0, -10);

    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_accept_limit_at_boundary() {
    Page page = new Page(0, 100);

    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_accept_limit_of_one() {
    Page page = new Page(0, 1);

    assertThat(page.getLimit(), is(1));
  }
}
