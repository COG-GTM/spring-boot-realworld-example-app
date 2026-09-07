package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_default_limit_to_twenty() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit(), is(20));
  }

  @Test
  public void should_keep_default_limit_for_zero_or_negative_values() {
    assertThat(new CursorPageParameter<>(null, 0, CursorPager.Direction.NEXT).getLimit(), is(20));
    assertThat(new CursorPageParameter<>(null, -1, CursorPager.Direction.NEXT).getLimit(), is(20));
  }

  @Test
  public void should_clamp_limit_to_one_thousand() {
    assertThat(
        new CursorPageParameter<>(null, 5000, CursorPager.Direction.NEXT).getLimit(), is(1000));
  }

  @Test
  public void should_keep_valid_limit() {
    CursorPageParameter<String> parameter =
        new CursorPageParameter<>("cursor", 50, CursorPager.Direction.NEXT);

    assertThat(parameter.getLimit(), is(50));
    assertThat(parameter.getQueryLimit(), is(51));
    assertThat(parameter.getCursor(), is("cursor"));
  }

  @Test
  public void should_identify_next_direction() {
    assertThat(new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT).isNext(), is(true));
    assertThat(new CursorPageParameter<>(null, 20, CursorPager.Direction.PREV).isNext(), is(false));
  }
}
