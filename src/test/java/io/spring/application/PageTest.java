package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_have_default_offset_0_and_limit_20() {
    Page page = new Page();
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_set_custom_offset_and_limit() {
    Page page = new Page(5, 10);
    assertEquals(5, page.getOffset());
    assertEquals(10, page.getLimit());
  }

  @Test
  public void should_cap_limit_at_max_100() {
    Page page = new Page(0, 200);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_not_set_negative_offset() {
    Page page = new Page(-5, 10);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_not_set_zero_offset_when_zero_provided() {
    Page page = new Page(0, 10);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_not_set_negative_limit() {
    Page page = new Page(0, -5);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_not_set_zero_limit() {
    Page page = new Page(0, 0);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_accept_limit_at_boundary() {
    Page page = new Page(0, 100);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_accept_limit_of_one() {
    Page page = new Page(0, 1);
    assertEquals(1, page.getLimit());
  }
}
