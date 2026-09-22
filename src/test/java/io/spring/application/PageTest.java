package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults() {
    Page page = new Page();
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_keep_valid_values() {
    Page page = new Page(10, 50);
    assertEquals(10, page.getOffset());
    assertEquals(50, page.getLimit());
  }

  @Test
  public void should_fall_back_to_defaults_for_non_positive_values() {
    Page page = new Page(-1, 0);
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_cap_limit_at_max() {
    assertEquals(100, new Page(0, 1000).getLimit());
  }
}
