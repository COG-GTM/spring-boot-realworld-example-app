package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_with_valid_parameters() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 10, CursorPager.Direction.NEXT);

    assertThat(param.getCursor(), is("cursor"));
    assertThat(param.getLimit(), is(10));
    assertThat(param.getDirection(), is(CursorPager.Direction.NEXT));
  }

  @Test
  public void should_return_true_for_next_direction() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 10, CursorPager.Direction.NEXT);

    assertThat(param.isNext(), is(true));
  }

  @Test
  public void should_return_false_for_prev_direction() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 10, CursorPager.Direction.PREV);

    assertThat(param.isNext(), is(false));
  }

  @Test
  public void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 10, CursorPager.Direction.NEXT);

    assertThat(param.getQueryLimit(), is(11));
  }

  @Test
  public void should_cap_limit_at_max() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 2000, CursorPager.Direction.NEXT);

    assertThat(param.getLimit(), is(1000));
    assertThat(param.getQueryLimit(), is(1001));
  }

  @Test
  public void should_ignore_negative_limit() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", -5, CursorPager.Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_ignore_zero_limit() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor", 0, CursorPager.Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_accept_null_cursor() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT);

    assertThat(param.getCursor(), nullValue());
  }
}
