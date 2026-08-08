package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_default_limit_when_created_without_arguments() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.getQueryLimit()).isEqualTo(21);
    assertThat(parameter.getCursor()).isNull();
    assertThat(parameter.getDirection()).isNull();
    assertThat(parameter.isNext()).isFalse();
  }

  @Test
  public void should_keep_cursor_direction_and_limit_from_constructor() {
    DateTime cursor = new DateTime(1000L);
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    assertThat(parameter.getCursor()).isEqualTo(cursor);
    assertThat(parameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(parameter.getLimit()).isEqualTo(10);
    assertThat(parameter.getQueryLimit()).isEqualTo(11);
    assertThat(parameter.isNext()).isTrue();
  }

  @Test
  public void should_not_be_next_for_previous_direction() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(null, 5, Direction.PREV);

    assertThat(parameter.isNext()).isFalse();
    assertThat(parameter.getCursor()).isNull();
  }

  @Test
  public void should_clamp_limit_above_max_to_max() {
    CursorPageParameter<String> parameter =
        new CursorPageParameter<>("cursor", 5000, Direction.NEXT);

    assertThat(parameter.getLimit()).isEqualTo(1000);
    assertThat(parameter.getQueryLimit()).isEqualTo(1001);
  }

  @Test
  public void should_fall_back_to_default_limit_for_non_positive_limit() {
    assertThat(new CursorPageParameter<>("cursor", 0, Direction.NEXT).getLimit()).isEqualTo(20);
    assertThat(new CursorPageParameter<>("cursor", -3, Direction.NEXT).getLimit()).isEqualTo(20);
  }

  @Test
  public void should_be_equal_for_same_values() {
    CursorPageParameter<String> one = new CursorPageParameter<>("c", 7, Direction.NEXT);
    CursorPageParameter<String> other = new CursorPageParameter<>("c", 7, Direction.NEXT);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("limit=7");
  }
}
