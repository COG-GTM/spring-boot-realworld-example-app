package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PageTest {

  @Test
  public void should_use_default_offset_and_limit_with_no_args_constructor() {
    Page page = new Page();

    assertThat(page.getOffset()).isEqualTo(0);
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @ParameterizedTest(name = "new Page({0}, {1}) -> offset={2}, limit={3}")
  @CsvSource({
    // offset, limit, expectedOffset, expectedLimit
    "0,    10,   0,   10",
    "5,    10,   5,   10",
    "10,   100,  10,  100",
    // offset <= 0 falls back to the default 0
    "-1,   10,   0,   10",
    "-100, 10,   0,   10",
    // limit <= 0 falls back to the default 20
    "5,    0,    5,   20",
    "5,    -1,   5,   20",
    // limit above MAX_LIMIT is clamped to 100
    "5,    101,  5,   100",
    "5,    5000, 5,   100",
  })
  public void should_clamp_offset_and_limit(
      int offset, int limit, int expectedOffset, int expectedLimit) {
    Page page = new Page(offset, limit);

    assertThat(page.getOffset()).isEqualTo(expectedOffset);
    assertThat(page.getLimit()).isEqualTo(expectedLimit);
  }

  @Test
  public void should_keep_limit_exactly_at_max_limit() {
    Page page = new Page(0, 100);

    assertThat(page.getLimit()).isEqualTo(100);
  }

  @Test
  public void should_be_equal_for_same_offset_and_limit() {
    Page page = new Page(3, 7);
    Page same = new Page(3, 7);
    Page other = new Page(3, 8);

    assertThat(page).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(page).isNotEqualTo(other);
    assertThat(page.toString()).contains("offset=3", "limit=7");
  }
}
