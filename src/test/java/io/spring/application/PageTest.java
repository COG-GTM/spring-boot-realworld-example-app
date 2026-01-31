package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_create_page_with_default_values() {
    Page page = new Page();

    assertThat(page.getOffset(), is(0));
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_create_page_with_custom_values() {
    Page page = new Page(10, 50);

    assertThat(page.getOffset(), is(10));
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_not_set_negative_offset() {
    Page page = new Page(-5, 20);

    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_not_set_zero_offset() {
    Page page = new Page(0, 20);

    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_set_positive_offset() {
    Page page = new Page(100, 20);

    assertThat(page.getOffset(), is(100));
  }

  @Test
  public void should_cap_limit_at_max_value() {
    Page page = new Page(0, 200);

    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_not_set_negative_limit() {
    Page page = new Page(0, -10);

    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_not_set_zero_limit() {
    Page page = new Page(0, 0);

    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_set_valid_limit() {
    Page page = new Page(0, 50);

    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_set_limit_at_max_boundary() {
    Page page = new Page(0, 100);

    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_cap_limit_just_above_max() {
    Page page = new Page(0, 101);

    assertThat(page.getLimit(), is(100));
  }
}
