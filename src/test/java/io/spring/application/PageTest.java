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
  public void should_set_offset_when_positive() {
    Page page = new Page(10, 20);
    
    assertThat(page.getOffset(), is(10));
  }

  @Test
  public void should_not_set_offset_when_negative() {
    Page page = new Page(-5, 20);
    
    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_not_set_offset_when_zero() {
    Page page = new Page(0, 20);
    
    assertThat(page.getOffset(), is(0));
  }

  @Test
  public void should_set_limit_when_positive_and_under_max() {
    Page page = new Page(0, 50);
    
    assertThat(page.getLimit(), is(50));
  }

  @Test
  public void should_cap_limit_at_max_when_exceeds() {
    Page page = new Page(0, 150);
    
    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_not_set_limit_when_negative() {
    Page page = new Page(0, -10);
    
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_not_set_limit_when_zero() {
    Page page = new Page(0, 0);
    
    assertThat(page.getLimit(), is(20));
  }

  @Test
  public void should_set_limit_to_max_when_exactly_max() {
    Page page = new Page(0, 100);
    
    assertThat(page.getLimit(), is(100));
  }

  @Test
  public void should_set_both_offset_and_limit_correctly() {
    Page page = new Page(25, 30);
    
    assertThat(page.getOffset(), is(25));
    assertThat(page.getLimit(), is(30));
  }
}
