package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_keep_valid_values() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 30, Direction.NEXT);
    assertThat(param.getCursor(), is("cursor"));
    assertThat(param.getLimit(), is(30));
    assertThat(param.getDirection(), is(Direction.NEXT));
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 5000, Direction.NEXT);
    assertThat(param.getLimit(), is(1000));
  }

  @Test
  public void should_fallback_to_default_limit_when_non_positive() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 0, Direction.NEXT);
    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_report_is_next_by_direction() {
    assertThat(new CursorPageParameter<>("c", 20, Direction.NEXT).isNext(), is(true));
    assertThat(new CursorPageParameter<>("c", 20, Direction.PREV).isNext(), is(false));
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 20, Direction.NEXT);
    assertThat(param.getQueryLimit(), is(21));
  }

  @Test
  public void should_default_with_no_args_constructor() {
    CursorPageParameter<String> param = new CursorPageParameter<>();
    assertThat(param.getLimit(), is(20));
    assertThat(param.getCursor(), is(nullValue()));
  }
}
