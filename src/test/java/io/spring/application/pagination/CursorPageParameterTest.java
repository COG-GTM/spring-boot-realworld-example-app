package io.spring.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class CursorPageParameterTest {

  @Test
  void should_keep_positive_limit_and_cursor() {
    DateTime cursor = new DateTime(1000L);

    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 5, Direction.NEXT);

    assertThat(parameter.getLimit()).isEqualTo(5);
    assertThat(parameter.getCursor()).isEqualTo(cursor);
    assertThat(parameter.getDirection()).isEqualTo(Direction.NEXT);
  }

  @Test
  void should_cap_limit_at_max() {
    CursorPageParameter<String> parameter =
        new CursorPageParameter<>("cursor", 5000, Direction.NEXT);

    assertThat(parameter.getLimit()).isEqualTo(1000);
  }

  @Test
  void should_fall_back_to_default_limit_for_non_positive_values() {
    assertThat(new CursorPageParameter<>("c", 0, Direction.NEXT).getLimit()).isEqualTo(20);
    assertThat(new CursorPageParameter<>("c", -3, Direction.NEXT).getLimit()).isEqualTo(20);
  }

  @Test
  void should_use_defaults_with_no_args_constructor() {
    CursorPageParameter<String> parameter = new CursorPageParameter<>();

    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.getCursor()).isNull();
    assertThat(parameter.getDirection()).isNull();
    assertThat(parameter.isNext()).isFalse();
  }

  @Test
  void should_tell_whether_direction_is_next() {
    assertThat(new CursorPageParameter<>("c", 10, Direction.NEXT).isNext()).isTrue();
    assertThat(new CursorPageParameter<>("c", 10, Direction.PREV).isNext()).isFalse();
  }

  @Test
  void should_query_one_extra_row_to_detect_further_pages() {
    assertThat(new CursorPageParameter<>("c", 10, Direction.NEXT).getQueryLimit()).isEqualTo(11);
  }

  @Test
  void should_implement_value_semantics() {
    CursorPageParameter<String> one = new CursorPageParameter<>("c", 10, Direction.NEXT);
    CursorPageParameter<String> same = new CursorPageParameter<>("c", 10, Direction.NEXT);
    CursorPageParameter<String> otherCursor = new CursorPageParameter<>("d", 10, Direction.NEXT);
    CursorPageParameter<String> otherLimit = new CursorPageParameter<>("c", 11, Direction.NEXT);
    CursorPageParameter<String> otherDirection = new CursorPageParameter<>("c", 10, Direction.PREV);

    assertThat(one).isEqualTo(one).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(one).isNotEqualTo(otherCursor).isNotEqualTo(otherLimit).isNotEqualTo(otherDirection);
    assertThat(one).isNotEqualTo(null).isNotEqualTo("c");
    assertThat(one.toString()).contains("cursor=c").contains("limit=10");
  }
}
