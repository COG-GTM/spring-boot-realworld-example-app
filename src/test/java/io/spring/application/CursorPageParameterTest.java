package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_defaults_with_no_args_constructor() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.getCursor()).isNull();
    assertThat(parameter.getDirection()).isNull();
    assertThat(parameter.getQueryLimit()).isEqualTo(21);
  }

  @Test
  public void should_keep_cursor_direction_and_valid_limit() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    assertThat(parameter.getCursor()).isEqualTo(cursor);
    assertThat(parameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(parameter.getLimit()).isEqualTo(10);
    assertThat(parameter.getQueryLimit()).isEqualTo(11);
  }

  @Test
  public void should_clamp_limit_above_max_limit() {
    CursorPageParameter<String> parameter =
        new CursorPageParameter<>("cursor", 5000, Direction.NEXT);

    assertThat(parameter.getLimit()).isEqualTo(1000);
    assertThat(parameter.getQueryLimit()).isEqualTo(1001);
  }

  @Test
  public void should_fall_back_to_default_limit_for_non_positive_limit() {
    assertThat(new CursorPageParameter<>("cursor", 0, Direction.NEXT).getLimit()).isEqualTo(20);
    assertThat(new CursorPageParameter<>("cursor", -1, Direction.NEXT).getLimit()).isEqualTo(20);
  }

  @Test
  public void should_report_next_only_for_next_direction() {
    assertThat(new CursorPageParameter<>("cursor", 10, Direction.NEXT).isNext()).isTrue();
    assertThat(new CursorPageParameter<>("cursor", 10, Direction.PREV).isNext()).isFalse();
    assertThat(new CursorPageParameter<>().isNext()).isFalse();
  }

  @Test
  public void should_allow_null_cursor() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>(null, 10, Direction.PREV);

    assertThat(parameter.getCursor()).isNull();
    assertThat(parameter.isNext()).isFalse();
  }

  @Test
  public void should_be_equal_for_same_cursor_limit_and_direction() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor", 10, Direction.NEXT);
    CursorPageParameter<String> same = new CursorPageParameter<>("cursor", 10, Direction.NEXT);

    assertThat(parameter).isEqualTo(same);
    assertThat(parameter.hashCode()).isEqualTo(same.hashCode());
    assertThat(parameter).isNotEqualTo(new CursorPageParameter<>("cursor", 10, Direction.PREV));
    assertThat(parameter).isNotEqualTo(null);
    assertThat(parameter.toString()).contains("cursor");
  }
}
