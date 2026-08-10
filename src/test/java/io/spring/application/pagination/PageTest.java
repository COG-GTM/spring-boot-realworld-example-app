package io.spring.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.Page;
import org.junit.jupiter.api.Test;

class PageTest {

  @Test
  void should_use_defaults_with_no_args_constructor() {
    Page page = new Page();

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  void should_keep_positive_offset_and_limit() {
    Page page = new Page(10, 30);

    assertThat(page.getOffset()).isEqualTo(10);
    assertThat(page.getLimit()).isEqualTo(30);
  }

  @Test
  void should_cap_limit_at_max() {
    assertThat(new Page(0, 500).getLimit()).isEqualTo(100);
  }

  @Test
  void should_fall_back_to_defaults_for_non_positive_values() {
    Page zero = new Page(0, 0);
    Page negative = new Page(-5, -5);

    assertThat(zero.getOffset()).isZero();
    assertThat(zero.getLimit()).isEqualTo(20);
    assertThat(negative.getOffset()).isZero();
    assertThat(negative.getLimit()).isEqualTo(20);
  }

  @Test
  void should_implement_value_semantics() {
    Page page = new Page(5, 10);
    Page same = new Page(5, 10);

    assertThat(page).isEqualTo(page).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(page).isNotEqualTo(new Page(6, 10)).isNotEqualTo(new Page(5, 11));
    assertThat(page).isNotEqualTo(null).isNotEqualTo("page");
    assertThat(page.toString()).contains("offset=5").contains("limit=10");
  }
}
