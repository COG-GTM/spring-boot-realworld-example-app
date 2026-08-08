package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PageTest {

  @Test
  public void should_use_defaults_when_created_without_arguments() {
    Page page = new Page();

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  public void should_keep_valid_offset_and_limit() {
    Page page = new Page(15, 30);

    assertThat(page.getOffset()).isEqualTo(15);
    assertThat(page.getLimit()).isEqualTo(30);
  }

  @Test
  public void should_clamp_limit_above_max() {
    assertThat(new Page(0, 500).getLimit()).isEqualTo(100);
  }

  @Test
  public void should_fall_back_to_defaults_for_non_positive_input() {
    Page zero = new Page(0, 0);
    Page negative = new Page(-5, -10);

    assertThat(zero.getOffset()).isZero();
    assertThat(zero.getLimit()).isEqualTo(20);
    assertThat(negative.getOffset()).isZero();
    assertThat(negative.getLimit()).isEqualTo(20);
  }

  @Test
  public void should_be_equal_for_same_offset_and_limit() {
    Page one = new Page(5, 10);
    Page other = new Page(5, 10);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("offset=5");
  }
}
