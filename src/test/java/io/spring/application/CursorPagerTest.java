package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class TestNode implements Node {
    private final DateTimeCursor cursor;

    TestNode(DateTime time) {
      this.cursor = new DateTimeCursor(time);
    }

    @Override
    public DateTimeCursor getCursor() {
      return cursor;
    }
  }

  private static TestNode node(long millis) {
    return new TestNode(new DateTime(millis, DateTimeZone.UTC));
  }

  @Test
  public void should_set_next_when_direction_next_and_has_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(node(3000), node(2000)), Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertTrue(pager.isNext());
    assertFalse(pager.hasPrevious());
    assertFalse(pager.isPrevious());
  }

  @Test
  public void should_not_set_next_when_direction_next_without_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.singletonList(node(1000)), Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_set_previous_when_direction_prev_and_has_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(node(3000), node(2000)), Direction.PREV, true);

    assertTrue(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_not_set_previous_when_direction_prev_without_extra() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.singletonList(node(1000)), Direction.PREV, false);

    assertFalse(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_treat_null_direction_as_previous() {
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.singletonList(node(1000)), null, true);

    assertTrue(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_return_start_and_end_cursor_from_first_and_last_items() {
    TestNode first = node(3000);
    TestNode middle = node(2000);
    TestNode last = node(1000);
    CursorPager<TestNode> pager =
        new CursorPager<>(Arrays.asList(first, middle, last), Direction.NEXT, false);

    assertSame(first.getCursor(), pager.getStartCursor());
    assertSame(last.getCursor(), pager.getEndCursor());
    assertEquals("3000", pager.getStartCursor().toString());
    assertEquals("1000", pager.getEndCursor().toString());
  }

  @Test
  public void should_return_same_start_and_end_cursor_for_single_item() {
    TestNode only = node(42);
    CursorPager<TestNode> pager =
        new CursorPager<>(Collections.singletonList(only), Direction.NEXT, false);

    assertSame(only.getCursor(), pager.getStartCursor());
    assertSame(only.getCursor(), pager.getEndCursor());
  }

  @Test
  public void should_return_null_cursors_for_empty_page() {
    List<TestNode> empty = Collections.emptyList();
    CursorPager<TestNode> pager = new CursorPager<>(empty, Direction.NEXT, false);

    assertTrue(pager.getData().isEmpty());
    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void should_expose_data_unchanged() {
    List<TestNode> data = Arrays.asList(node(2), node(1));
    CursorPager<TestNode> pager = new CursorPager<>(data, Direction.PREV, false);

    assertSame(data, pager.getData());
  }

  @Test
  public void date_time_cursor_should_serialize_to_epoch_millis() {
    DateTime time = new DateTime(2021, 3, 4, 5, 6, 7, 890, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(time);

    assertEquals(String.valueOf(time.getMillis()), cursor.toString());
    assertSame(time, cursor.getData());
  }

  @Test
  public void date_time_cursor_should_round_trip_through_parse() {
    DateTime time = new DateTime(2022, 1, 2, 3, 4, 5, 678, DateTimeZone.forOffsetHours(8));
    String serialized = new DateTimeCursor(time).toString();

    DateTime parsed = DateTimeCursor.parse(serialized);

    assertEquals(time.getMillis(), parsed.getMillis());
    assertEquals(DateTimeZone.UTC, parsed.getZone());
    assertTrue(time.isEqual(parsed));
  }

  @Test
  public void date_time_cursor_parse_should_return_null_for_null_cursor() {
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void date_time_cursor_parse_should_reject_non_numeric_cursor() {
    org.junit.jupiter.api.Assertions.assertThrows(
        NumberFormatException.class, () -> DateTimeCursor.parse("not-a-cursor"));
  }

  @Test
  public void page_cursor_should_delegate_to_string_to_data() {
    PageCursor<String> cursor = new PageCursor<String>("abc") {};

    assertEquals("abc", cursor.getData());
    assertEquals("abc", cursor.toString());
  }

  @Test
  public void cursor_page_parameter_should_request_one_extra_row() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 10, Direction.NEXT);

    assertEquals(10, param.getLimit());
    assertEquals(11, param.getQueryLimit());
    assertTrue(param.isNext());
  }

  @Test
  public void cursor_page_parameter_should_cap_limit_at_max() {
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(null, 5000, Direction.PREV);

    assertEquals(1000, param.getLimit());
    assertEquals(1001, param.getQueryLimit());
    assertFalse(param.isNext());
  }

  @Test
  public void cursor_page_parameter_should_keep_default_limit_for_non_positive_values() {
    assertEquals(20, new CursorPageParameter<>(null, 0, Direction.NEXT).getLimit());
    assertEquals(20, new CursorPageParameter<>(null, -5, Direction.NEXT).getLimit());
  }

  @Test
  public void cursor_page_parameter_should_keep_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> param = new CursorPageParameter<>(cursor, 5, Direction.NEXT);

    assertSame(cursor, param.getCursor());
  }
}
