package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class FakeNode implements Node {
    private final DateTime at;

    FakeNode(long millis) {
      this.at = new DateTime(millis, DateTimeZone.UTC);
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(at);
    }
  }

  @Test
  public void next_direction_should_report_next_when_extra() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode(1000)), Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void prev_direction_should_report_previous_when_extra() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode(1000)), Direction.PREV, true);

    assertFalse(pager.hasNext());
    assertTrue(pager.hasPrevious());
  }

  @Test
  public void cursors_should_be_null_for_empty_data() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
    assertFalse(pager.hasNext());
  }

  @Test
  public void cursors_should_come_from_first_and_last_elements() {
    List<FakeNode> data = Arrays.asList(new FakeNode(1000), new FakeNode(2000));
    CursorPager<FakeNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals("1000", pager.getStartCursor().toString());
    assertEquals("2000", pager.getEndCursor().toString());
  }

  @Test
  public void datetime_cursor_should_parse_millis() {
    DateTime parsed = DateTimeCursor.parse("5000");
    assertEquals(5000, parsed.getMillis());
    assertNull(DateTimeCursor.parse(null));
  }

  @Test
  public void cursor_page_parameter_should_clamp_limit() {
    CursorPageParameter<DateTime> param =
        new CursorPageParameter<>(null, 5000, Direction.NEXT);
    assertEquals(1000, param.getLimit());
    assertEquals(1001, param.getQueryLimit());
    assertTrue(param.isNext());

    CursorPageParameter<DateTime> defaulted =
        new CursorPageParameter<>(null, -1, Direction.PREV);
    assertEquals(20, defaulted.getLimit());
    assertFalse(defaulted.isNext());
  }

  @Test
  public void page_should_clamp_offset_and_limit() {
    Page page = new Page(-5, 500);
    assertEquals(0, page.getOffset());
    assertEquals(100, page.getLimit());

    Page normal = new Page(10, 30);
    assertEquals(10, normal.getOffset());
    assertEquals(30, normal.getLimit());
  }
}
