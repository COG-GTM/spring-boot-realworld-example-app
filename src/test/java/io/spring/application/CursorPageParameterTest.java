package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_default_values() {
    CursorPageParameter<String> param = new CursorPageParameter<>();

    assertThat(param.getLimit(), is(20));
    assertThat(param.getCursor(), nullValue());
    assertThat(param.getDirection(), nullValue());
  }

  @Test
  public void should_create_with_custom_values() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor123", 50, Direction.NEXT);

    assertThat(param.getCursor(), is("cursor123"));
    assertThat(param.getLimit(), is(50));
    assertThat(param.getDirection(), is(Direction.NEXT));
  }

  @Test
  public void should_cap_limit_at_max_value() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 2000, Direction.NEXT);

    assertThat(param.getLimit(), is(1000));
  }

  @Test
  public void should_use_default_limit_for_negative_value() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", -10, Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_use_default_limit_for_zero_value() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 0, Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_return_true_for_next_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 20, Direction.NEXT);

    assertThat(param.isNext(), is(true));
  }

  @Test
  public void should_return_false_for_prev_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 20, Direction.PREV);

    assertThat(param.isNext(), is(false));
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 50, Direction.NEXT);

    assertThat(param.getQueryLimit(), is(51));
  }

  @Test
  public void should_accept_limit_at_max_boundary() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 1000, Direction.NEXT);

    assertThat(param.getLimit(), is(1000));
  }
}
