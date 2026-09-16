package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults_with_no_args_constructor() {
    Page page = new Page();
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_accept_valid_offset_and_limit() {
    Page page = new Page(10, 50);
    assertEquals(10, page.getOffset());
    assertEquals(50, page.getLimit());
  }

  @Test
  public void should_ignore_negative_offset() {
    Page page = new Page(-5, 10);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_ignore_non_positive_limit() {
    assertEquals(20, new Page(0, 0).getLimit());
    assertEquals(20, new Page(0, -1).getLimit());
  }

  @Test
  public void should_cap_limit_at_max() {
    Page page = new Page(0, 1000);
    assertEquals(100, page.getLimit());
  }
}
