package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_default_offset_and_limit() {
    Page page = new Page();

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  public void should_keep_valid_offset_and_limit() {
    Page page = new Page(10, 50);

    assertThat(page.getOffset()).isEqualTo(10);
    assertThat(page.getLimit()).isEqualTo(50);
  }

  @Test
  public void should_fallback_to_defaults_for_non_positive_values() {
    Page page = new Page(-1, 0);

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    assertThat(new Page(0, 1000).getLimit()).isEqualTo(100);
  }

  @Test
  public void should_implement_equals_and_to_string() {
    assertThat(new Page(1, 5)).isEqualTo(new Page(1, 5)).hasSameHashCodeAs(new Page(1, 5));
    assertThat(new Page(1, 5)).isNotEqualTo(new Page(2, 5));
    assertThat(new Page(1, 5).toString()).contains("offset=1", "limit=5");
  }
}
