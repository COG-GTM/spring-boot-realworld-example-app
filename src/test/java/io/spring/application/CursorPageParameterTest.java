package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_default_limit_when_not_specified() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.getQueryLimit()).isEqualTo(21);
    assertThat(parameter.getCursor()).isNull();
  }

  @Test
  public void should_keep_cursor_limit_and_direction() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 5, Direction.NEXT);

    assertThat(parameter.getCursor()).isEqualTo(cursor);
    assertThat(parameter.getLimit()).isEqualTo(5);
    assertThat(parameter.getQueryLimit()).isEqualTo(6);
    assertThat(parameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(parameter.isNext()).isTrue();
  }

  @Test
  public void should_not_be_next_for_prev_direction() {
    assertThat(new CursorPageParameter<>(null, 5, Direction.PREV).isNext()).isFalse();
  }

  @Test
  public void should_cap_limit_at_max_limit_and_ignore_non_positive_limit() {
    assertThat(new CursorPageParameter<>(null, 5000, Direction.NEXT).getLimit()).isEqualTo(1000);
    assertThat(new CursorPageParameter<>(null, 0, Direction.NEXT).getLimit()).isEqualTo(20);
  }

  @Test
  public void should_implement_equals_and_to_string() {
    CursorPageParameter<String> one = new CursorPageParameter<>("a", 3, Direction.NEXT);
    CursorPageParameter<String> other = new CursorPageParameter<>("a", 3, Direction.NEXT);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one).isNotEqualTo(new CursorPageParameter<>("b", 3, Direction.NEXT));
    assertThat(one.toString()).contains("cursor=a", "limit=3");
  }
}
