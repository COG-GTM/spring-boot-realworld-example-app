package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_create_with_defaults() {
    Page page = new Page();
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_create_with_valid_params() {
    Page page = new Page(5, 10);
    assertEquals(5, page.getOffset());
    assertEquals(10, page.getLimit());
  }

  @Test
  public void should_cap_limit_at_100() {
    Page page = new Page(0, 200);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_keep_default_for_zero_offset() {
    Page page = new Page(0, 10);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_keep_default_for_negative_offset() {
    Page page = new Page(-5, 10);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_keep_default_for_zero_limit() {
    Page page = new Page(0, 0);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_keep_default_for_negative_limit() {
    Page page = new Page(0, -5);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_accept_valid_offset() {
    Page page = new Page(10, 20);
    assertEquals(10, page.getOffset());
  }

  @Test
  public void should_accept_limit_equal_to_max() {
    Page page = new Page(0, 100);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_accept_limit_of_one() {
    Page page = new Page(0, 1);
    assertEquals(1, page.getLimit());
  }
}
