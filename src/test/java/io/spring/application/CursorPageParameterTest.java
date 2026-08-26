package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_defaults_for_empty_parameter() {
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>();
    assertThat(parameter.getLimit(), is(20));
    assertThat(parameter.getCursor(), nullValue());
  }

  @Test
  public void should_keep_valid_limit_and_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 30, Direction.NEXT);

    assertThat(parameter.getLimit(), is(30));
    assertThat(parameter.getCursor(), is(cursor));
    assertThat(parameter.isNext(), is(true));
    assertThat(parameter.getQueryLimit(), is(31));
  }

  @Test
  public void should_cap_limit_at_max() {
    assertThat(new CursorPageParameter<>(null, 2000, Direction.NEXT).getLimit(), is(1000));
  }

  @Test
  public void should_fall_back_to_default_limit_for_non_positive_value() {
    assertThat(new CursorPageParameter<>(null, 0, Direction.PREV).getLimit(), is(20));
  }

  @Test
  public void should_not_be_next_for_prev_direction() {
    assertThat(new CursorPageParameter<>(null, 20, Direction.PREV).isNext(), is(false));
  }
}
