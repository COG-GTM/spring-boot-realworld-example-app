package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void default_page_should_have_zero_offset_and_limit_twenty() {
    Page page = new Page();

    assertThat(page.getOffset()).isEqualTo(0);
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  public void should_accept_valid_offset_and_limit() {
    Page page = new Page(5, 30);

    assertThat(page.getOffset()).isEqualTo(5);
    assertThat(page.getLimit()).isEqualTo(30);
  }

  @Test
  public void should_ignore_non_positive_offset() {
    assertThat(new Page(0, 10).getOffset()).isEqualTo(0);
    assertThat(new Page(-5, 10).getOffset()).isEqualTo(0);
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    Page page = new Page(0, 500);

    assertThat(page.getLimit()).isEqualTo(100);
  }

  @Test
  public void should_keep_default_limit_when_limit_not_positive() {
    assertThat(new Page(0, 0).getLimit()).isEqualTo(20);
    assertThat(new Page(0, -1).getLimit()).isEqualTo(20);
  }

  @Test
  public void should_accept_limit_at_max_boundary() {
    assertThat(new Page(0, 100).getLimit()).isEqualTo(100);
  }
}
