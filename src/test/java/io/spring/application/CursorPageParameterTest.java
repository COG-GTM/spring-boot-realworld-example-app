package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_store_cursor_limit_and_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>("cursor", 30, Direction.NEXT);

    assertThat(param.getCursor()).isEqualTo("cursor");
    assertThat(param.getLimit()).isEqualTo(30);
    assertThat(param.getDirection()).isEqualTo(Direction.NEXT);
  }

  @Test
  public void is_next_should_reflect_direction() {
    assertThat(new CursorPageParameter<>(null, 20, Direction.NEXT).isNext()).isTrue();
    assertThat(new CursorPageParameter<>(null, 20, Direction.PREV).isNext()).isFalse();
  }

  @Test
  public void query_limit_should_be_limit_plus_one() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 20, Direction.NEXT);

    assertThat(param.getQueryLimit()).isEqualTo(21);
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5000, Direction.NEXT);

    assertThat(param.getLimit()).isEqualTo(1000);
    assertThat(param.getQueryLimit()).isEqualTo(1001);
  }

  @Test
  public void should_keep_default_limit_when_limit_not_positive() {
    assertThat(new CursorPageParameter<>(null, 0, Direction.NEXT).getLimit()).isEqualTo(20);
    assertThat(new CursorPageParameter<>(null, -3, Direction.NEXT).getLimit()).isEqualTo(20);
  }

  @Test
  public void default_constructor_should_use_default_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>();

    assertThat(param.getLimit()).isEqualTo(20);
    assertThat(param.getCursor()).isNull();
    assertThat(param.getDirection()).isNull();
  }

  @Test
  public void should_allow_null_cursor() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 10, Direction.PREV);

    assertThat(param.getCursor()).isNull();
  }
}
