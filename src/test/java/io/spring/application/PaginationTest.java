package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

class PaginationTest {

  private Node node(DateTime dateTime) {
    return () -> new DateTimeCursor(dateTime);
  }

  @Test
  void should_expose_next_page_only_when_paging_forward() {
    List<Node> data = Arrays.asList(node(new DateTime()), node(new DateTime()));

    CursorPager<Node> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.hasNext()).isTrue();
    assertThat(pager.hasPrevious()).isFalse();
    assertThat(pager.getData()).hasSize(2);
  }

  @Test
  void should_expose_previous_page_only_when_paging_backward() {
    CursorPager<Node> pager =
        new CursorPager<>(Collections.singletonList(node(new DateTime())), Direction.PREV, true);

    assertThat(pager.hasNext()).isFalse();
    assertThat(pager.hasPrevious()).isTrue();
  }

  @Test
  void should_return_first_and_last_cursor_of_data() {
    DateTime first = new DateTime(1000L);
    DateTime last = new DateTime(2000L);
    CursorPager<Node> pager =
        new CursorPager<>(Arrays.asList(node(first), node(last)), Direction.NEXT, false);

    assertThat(pager.getStartCursor().toString()).isEqualTo("1000");
    assertThat(pager.getEndCursor().toString()).isEqualTo("2000");
  }

  @Test
  void should_return_null_cursors_when_empty() {
    CursorPager<Node> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getStartCursor()).isNull();
    assertThat(pager.getEndCursor()).isNull();
  }

  @Test
  void should_print_cursor_as_millis_and_parse_it_back() {
    DateTime now = new DateTime();
    DateTimeCursor cursor = new DateTimeCursor(now);

    assertThat(cursor.toString()).isEqualTo(String.valueOf(now.getMillis()));
    assertThat(cursor.getData()).isEqualTo(now);

    DateTime parsed = DateTimeCursor.parse(cursor.toString());
    assertThat(parsed.getMillis()).isEqualTo(now.getMillis());
    assertThat(parsed.getZone()).isEqualTo(DateTimeZone.UTC);
  }

  @Test
  void should_parse_null_cursor_as_null() {
    assertThat(DateTimeCursor.parse(null)).isNull();
  }

  @Test
  void should_use_generic_page_cursor_to_string() {
    PageCursor<String> cursor = new PageCursor<String>("abc") {};

    assertThat(cursor.toString()).isEqualTo("abc");
    assertThat(cursor.getData()).isEqualTo("abc");
  }

  @Test
  void should_use_default_offset_and_limit() {
    Page page = new Page();

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  void should_keep_valid_offset_and_limit() {
    Page page = new Page(10, 50);

    assertThat(page.getOffset()).isEqualTo(10);
    assertThat(page.getLimit()).isEqualTo(50);
  }

  @Test
  void should_fallback_to_defaults_for_invalid_offset_and_limit() {
    Page page = new Page(-1, 0);

    assertThat(page.getOffset()).isZero();
    assertThat(page.getLimit()).isEqualTo(20);
  }

  @Test
  void should_cap_page_limit() {
    assertThat(new Page(0, 1000).getLimit()).isEqualTo(100);
  }

  @Test
  void should_build_cursor_page_parameter() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(cursor, 10, Direction.NEXT);

    assertThat(parameter.getCursor()).isEqualTo(cursor);
    assertThat(parameter.getLimit()).isEqualTo(10);
    assertThat(parameter.getQueryLimit()).isEqualTo(11);
    assertThat(parameter.isNext()).isTrue();
  }

  @Test
  void should_cap_cursor_page_parameter_limit_and_default_invalid_limit() {
    assertThat(new CursorPageParameter<>(null, 5000, Direction.PREV).getLimit()).isEqualTo(1000);
    CursorPageParameter<DateTime> parameter = new CursorPageParameter<>(null, 0, Direction.PREV);
    assertThat(parameter.getLimit()).isEqualTo(20);
    assertThat(parameter.isNext()).isFalse();
  }
}
