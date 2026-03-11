package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_cursor_page_parameter() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 10, Direction.NEXT);

    assertThat(param.getCursor(), is("cursor"));
    assertThat(param.getLimit(), is(10));
    assertThat(param.getDirection(), is(Direction.NEXT));
  }

  @Test
  public void should_create_empty_cursor_page_parameter() {
    CursorPageParameter<String> param = new CursorPageParameter<>();

    assertNotNull(param);
    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_check_if_next_direction() {
    CursorPageParameter<String> nextParam =
        new CursorPageParameter<>("cursor", 10, Direction.NEXT);
    CursorPageParameter<String> prevParam =
        new CursorPageParameter<>("cursor", 10, Direction.PREV);

    assertThat(nextParam.isNext(), is(true));
    assertThat(prevParam.isNext(), is(false));
  }

  @Test
  public void should_get_query_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 10, Direction.NEXT);

    assertThat(param.getQueryLimit(), is(11));
  }

  @Test
  public void should_limit_max_limit_to_1000() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 2000, Direction.NEXT);

    assertThat(param.getLimit(), is(1000));
  }

  @Test
  public void should_ignore_negative_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", -10, Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_ignore_zero_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 0, Direction.NEXT);

    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_accept_valid_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 50, Direction.NEXT);

    assertThat(param.getLimit(), is(50));
  }
}
