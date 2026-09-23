package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_keep_given_cursor_limit_and_direction() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor", 10, Direction.NEXT);

    assertThat(parameter.getCursor(), is("cursor"));
    assertThat(parameter.getLimit(), is(10));
    assertThat(parameter.getDirection(), is(Direction.NEXT));
    assertThat(parameter.isNext(), is(true));
  }

  @Test
  public void should_not_be_next_when_direction_is_prev() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>(null, 10, Direction.PREV);

    assertThat(parameter.getCursor(), is(nullValue()));
    assertThat(parameter.isNext(), is(false));
  }

  @Test
  public void should_query_one_more_than_limit() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertThat(parameter.getQueryLimit(), is(11));
  }

  @Test
  public void should_fallback_to_default_limit_when_limit_is_not_positive() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>(null, 0, Direction.NEXT);
    assertThat(parameter.getLimit(), is(20));
  }

  @Test
  public void should_cap_limit_to_max() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>(null, 5000, Direction.NEXT);
    assertThat(parameter.getLimit(), is(1000));
  }
}
