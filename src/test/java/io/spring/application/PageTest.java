package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_create_page_with_default_values() {
    Page page = new Page();

    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_create_page_with_custom_values() {
    Page page = new Page(10, 50);

    assertEquals(10, page.getOffset());
    assertEquals(50, page.getLimit());
  }

  @Test
  public void should_not_set_negative_offset() {
    Page page = new Page(-5, 20);

    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_not_set_zero_offset_when_zero_provided() {
    Page page = new Page(0, 20);

    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_cap_limit_at_max_100() {
    Page page = new Page(0, 150);

    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_not_set_negative_limit() {
    Page page = new Page(0, -10);

    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_not_set_zero_limit() {
    Page page = new Page(0, 0);

    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_accept_limit_at_max_boundary() {
    Page page = new Page(0, 100);

    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_accept_valid_positive_offset() {
    Page page = new Page(50, 20);

    assertEquals(50, page.getOffset());
  }
}
