package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_set_cursor_limit_and_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", 10, Direction.NEXT);
    assertThat(param.getCursor(), is("abc"));
    assertThat(param.getLimit(), is(10));
    assertThat(param.getDirection(), is(Direction.NEXT));
  }

  @Test
  public void should_return_true_for_is_next_when_direction_is_next() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", 10, Direction.NEXT);
    assertThat(param.isNext(), is(true));
  }

  @Test
  public void should_return_false_for_is_next_when_direction_is_prev() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", 10, Direction.PREV);
    assertThat(param.isNext(), is(false));
  }

  @Test
  public void should_return_limit_plus_one_for_query_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", 10, Direction.NEXT);
    assertThat(param.getQueryLimit(), is(11));
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", 2000, Direction.NEXT);
    assertThat(param.getLimit(), is(1000));
  }

  @Test
  public void should_keep_default_limit_when_negative() {
    CursorPageParameter<String> param = new CursorPageParameter<>("abc", -1, Direction.NEXT);
    assertThat(param.getLimit(), is(20));
  }
}
