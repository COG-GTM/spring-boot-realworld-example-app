package io.spring.application.pagination;

import io.spring.application.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_create_with_default_values() {
    Page page = new Page();

    Assertions.assertEquals(0, page.getOffset());
    Assertions.assertEquals(20, page.getLimit());
  }

  @Test
  public void should_create_with_custom_values() {
    Page page = new Page(10, 50);

    Assertions.assertEquals(10, page.getOffset());
    Assertions.assertEquals(50, page.getLimit());
  }

  @Test
  public void should_cap_limit_at_max_value() {
    Page page = new Page(0, 200);

    Assertions.assertEquals(100, page.getLimit());
  }

  @Test
  public void should_keep_limit_when_below_max() {
    Page page = new Page(0, 50);

    Assertions.assertEquals(50, page.getLimit());
  }

  @Test
  public void should_keep_default_limit_when_zero_or_negative() {
    Page page1 = new Page(0, 0);
    Page page2 = new Page(0, -10);

    Assertions.assertEquals(20, page1.getLimit());
    Assertions.assertEquals(20, page2.getLimit());
  }

  @Test
  public void should_keep_default_offset_when_negative() {
    Page page = new Page(-5, 20);

    Assertions.assertEquals(0, page.getOffset());
  }

  @Test
  public void should_set_offset_when_positive() {
    Page page = new Page(25, 20);

    Assertions.assertEquals(25, page.getOffset());
  }

  @Test
  public void should_handle_limit_at_exact_max() {
    Page page = new Page(0, 100);

    Assertions.assertEquals(100, page.getLimit());
  }

  @Test
  public void should_handle_limit_just_above_max() {
    Page page = new Page(0, 101);

    Assertions.assertEquals(100, page.getLimit());
  }

  @Test
  public void should_handle_zero_offset() {
    Page page = new Page(0, 20);

    Assertions.assertEquals(0, page.getOffset());
  }

  @Test
  public void should_handle_large_offset() {
    Page page = new Page(1000, 20);

    Assertions.assertEquals(1000, page.getOffset());
  }

  @Test
  public void should_handle_both_invalid_values() {
    Page page = new Page(-10, -20);

    Assertions.assertEquals(0, page.getOffset());
    Assertions.assertEquals(20, page.getLimit());
  }
}
