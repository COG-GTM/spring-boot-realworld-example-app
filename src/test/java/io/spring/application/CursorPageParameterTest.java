package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

public class CursorPageParameterTest {

  @Test
  public void should_use_default_limit_with_no_args_constructor() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.getQueryLimit()).isEqualTo(21);
    assertThat(parameter.getCursor()).isNull();
    assertThat(parameter.getDirection()).isNull();
    assertThat(parameter.isNext()).isFalse();
  }

  @ParameterizedTest(name = "limit {0} -> limit={1}, queryLimit={2}")
  @CsvSource({
    "1,     1,    2",
    "20,    20,   21",
    "1000,  1000, 1001",
    // limit above MAX_LIMIT is clamped to 1000
    "1001,  1000, 1001",
    "999999, 1000, 1001",
    // limit <= 0 falls back to the default 20
    "0,     20,   21",
    "-1,    20,   21",
  })
  public void should_clamp_limit_and_fetch_one_extra_row(
      int limit, int expectedLimit, int expectedQueryLimit) {
    CursorPageParameter<DateTime> parameter =
        new CursorPageParameter<>(DateTime.now(), limit, Direction.NEXT);

    assertThat(parameter.getLimit()).isEqualTo(expectedLimit);
    assertThat(parameter.getQueryLimit()).isEqualTo(expectedQueryLimit);
  }

  @Test
  public void should_keep_the_given_cursor() {
    DateTime cursor = new DateTime(1_600_000_000_000L);

    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    assertThat(parameter.getCursor()).isEqualTo(cursor);
  }

  @Test
  public void should_allow_null_cursor_for_the_first_page() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(null, 10, Direction.NEXT);

    assertThat(parameter.getCursor()).isNull();
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  public void should_report_next_only_for_next_direction(Direction direction) {
    CursorPageParameter<DateTime> parameter =
        new CursorPageParameter<>(DateTime.now(), 10, direction);

    assertThat(parameter.getDirection()).isEqualTo(direction);
    assertThat(parameter.isNext()).isEqualTo(direction == Direction.NEXT);
  }

  @Test
  public void should_expose_both_directions_in_the_enum() {
    assertThat(Direction.values()).containsExactly(Direction.PREV, Direction.NEXT);
    assertThat(Direction.valueOf("NEXT")).isEqualTo(Direction.NEXT);
    assertThat(Direction.valueOf("PREV")).isEqualTo(Direction.PREV);
  }

  @Test
  public void should_be_equal_for_same_cursor_limit_and_direction() {
    DateTime cursor = new DateTime(1_600_000_000_000L);
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    CursorPageParameter<DateTime> same = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    CursorPageParameter<DateTime> other = new CursorPageParameter<>(cursor, 10, Direction.PREV);

    assertThat(parameter).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(parameter).isNotEqualTo(other);
  }
}
