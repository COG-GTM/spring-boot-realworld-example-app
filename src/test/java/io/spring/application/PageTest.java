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
  public void should_clamp_limit_above_max_limit() {
    assertThat(new Page(0, 101).getLimit()).isEqualTo(100);
    assertThat(new Page(0, 1000).getLimit()).isEqualTo(100);
    assertThat(new Page(0, 100).getLimit()).isEqualTo(100);
  }

  @Test
  public void should_fall_back_to_default_limit_for_non_positive_limit() {
    assertThat(new Page(0, 0).getLimit()).isEqualTo(20);
    assertThat(new Page(0, -5).getLimit()).isEqualTo(20);
  }

  @Test
  public void should_fall_back_to_zero_offset_for_non_positive_offset() {
    assertThat(new Page(0, 20).getOffset()).isZero();
    assertThat(new Page(-10, 20).getOffset()).isZero();
  }

  @Test
  public void should_be_equal_for_same_offset_and_limit() {
    Page page = new Page(10, 50);
    Page same = new Page(10, 50);

    assertThat(page).isEqualTo(same);
    assertThat(page.hashCode()).isEqualTo(same.hashCode());
    assertThat(page).isNotEqualTo(new Page(11, 50));
    assertThat(page).isNotEqualTo(null);
    assertThat(page.toString()).contains("10", "50");
  }
}
