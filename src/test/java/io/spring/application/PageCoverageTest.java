package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PageCoverageTest {

  @Test
  public void should_create_with_defaults() {
    Page page = new Page();
    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_create_with_constructor() {
    Page page = new Page(10, 50);
    assertEquals(10, page.getOffset());
    assertEquals(50, page.getLimit());
  }

  @Test
  public void should_cap_limit_at_max() {
    Page page = new Page(0, 200);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_not_accept_negative_offset() {
    Page page = new Page(-1, 20);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_not_accept_negative_limit() {
    Page page = new Page(0, -1);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_not_accept_zero_limit() {
    Page page = new Page(0, 0);
    assertEquals(20, page.getLimit());
  }

  @Test
  public void should_accept_zero_offset() {
    Page page = new Page(0, 20);
    assertEquals(0, page.getOffset());
  }

  @Test
  public void should_accept_positive_offset() {
    Page page = new Page(5, 20);
    assertEquals(5, page.getOffset());
  }

  @Test
  public void should_accept_exact_max_limit() {
    Page page = new Page(0, 100);
    assertEquals(100, page.getLimit());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    Page page1 = new Page(0, 20);
    Page page2 = new Page(0, 20);
    assertEquals(page1, page2);
    assertEquals(page1.hashCode(), page2.hashCode());
  }

  @Test
  public void should_have_toString() {
    Page page = new Page(5, 10);
    assertNotNull(page.toString());
  }
}
