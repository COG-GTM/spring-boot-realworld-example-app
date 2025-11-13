package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_create_cursor_page_parameter() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 10, Direction.NEXT);
    
    assertThat(parameter.getCursor(), is("cursor123"));
    assertThat(parameter.getLimit(), is(10));
    assertThat(parameter.getDirection(), is(Direction.NEXT));
  }

  @Test
  public void should_return_true_for_next_direction() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 10, Direction.NEXT);
    
    assertThat(parameter.isNext(), is(true));
  }

  @Test
  public void should_return_false_for_prev_direction() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 10, Direction.PREV);
    
    assertThat(parameter.isNext(), is(false));
  }

  @Test
  public void should_return_query_limit_plus_one() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 10, Direction.NEXT);
    
    assertThat(parameter.getQueryLimit(), is(11));
  }

  @Test
  public void should_limit_to_max_when_exceeds() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 2000, Direction.NEXT);
    
    assertThat(parameter.getLimit(), is(1000));
  }

  @Test
  public void should_use_default_limit_when_zero() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", 0, Direction.NEXT);
    
    assertThat(parameter.getLimit(), is(20));
  }

  @Test
  public void should_use_default_limit_when_negative() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>("cursor123", -5, Direction.NEXT);
    
    assertThat(parameter.getLimit(), is(20));
  }
}
