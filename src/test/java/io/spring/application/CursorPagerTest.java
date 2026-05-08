package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private static class FakeCursor extends PageCursor<String> {
    public FakeCursor(String data) {
      super(data);
    }
  }

  private static class FakeNode implements Node {
    private final FakeCursor cursor;

    FakeNode(String value) {
      this.cursor = new FakeCursor(value);
    }

    @Override
    public PageCursor getCursor() {
      return cursor;
    }
  }

  @Test
  public void next_with_extra_should_set_hasNext_true() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode("a")), Direction.NEXT, true);

    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void next_without_extra_should_have_no_next() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode("a")), Direction.NEXT, false);

    assertFalse(pager.hasNext());
    assertFalse(pager.hasPrevious());
  }

  @Test
  public void prev_with_extra_should_set_hasPrevious_true() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode("a")), Direction.PREV, true);

    assertTrue(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void prev_without_extra_should_have_no_previous() {
    CursorPager<FakeNode> pager =
        new CursorPager<>(Collections.singletonList(new FakeNode("a")), Direction.PREV, false);

    assertFalse(pager.hasPrevious());
    assertFalse(pager.hasNext());
  }

  @Test
  public void cursors_for_empty_data_should_be_null() {
    CursorPager<FakeNode> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertNull(pager.getStartCursor());
    assertNull(pager.getEndCursor());
  }

  @Test
  public void cursors_for_non_empty_data_should_be_first_and_last() {
    List<FakeNode> data =
        Arrays.asList(new FakeNode("first"), new FakeNode("middle"), new FakeNode("last"));
    CursorPager<FakeNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals("first", pager.getStartCursor().toString());
    assertEquals("last", pager.getEndCursor().toString());
  }

  @Test
  public void getData_should_return_supplied_list() {
    List<FakeNode> data = Arrays.asList(new FakeNode("first"), new FakeNode("last"));
    CursorPager<FakeNode> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertEquals(2, pager.getData().size());
  }
}
