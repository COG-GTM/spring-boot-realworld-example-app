package io.spring.application;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private CommentData comment(String id, DateTime createdAt) {
    return new CommentData(id, "body", "article", createdAt, createdAt, null);
  }

  @Test
  public void should_have_next_page_when_direction_is_next_and_has_extra() {
    DateTime first = new DateTime(1000L);
    DateTime last = new DateTime(2000L);
    CursorPager<CommentData> pager =
        new CursorPager<>(
            Arrays.asList(comment("1", first), comment("2", last)), Direction.NEXT, true);

    Assertions.assertTrue(pager.hasNext());
    Assertions.assertFalse(pager.hasPrevious());
    Assertions.assertEquals("1000", pager.getStartCursor().toString());
    Assertions.assertEquals("2000", pager.getEndCursor().toString());
  }

  @Test
  public void should_have_previous_page_when_direction_is_prev_and_has_extra() {
    CursorPager<CommentData> pager =
        new CursorPager<>(
            Collections.singletonList(comment("1", new DateTime(1000L))), Direction.PREV, true);

    Assertions.assertFalse(pager.hasNext());
    Assertions.assertTrue(pager.hasPrevious());
  }

  @Test
  public void should_not_have_more_pages_without_extra() {
    CursorPager<CommentData> next =
        new CursorPager<>(
            Collections.singletonList(comment("1", new DateTime())), Direction.NEXT, false);
    CursorPager<CommentData> prev =
        new CursorPager<>(
            Collections.singletonList(comment("1", new DateTime())), Direction.PREV, false);

    Assertions.assertFalse(next.hasNext());
    Assertions.assertFalse(next.hasPrevious());
    Assertions.assertFalse(prev.hasNext());
    Assertions.assertFalse(prev.hasPrevious());
  }

  @Test
  public void should_return_null_cursors_for_empty_page() {
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    Assertions.assertNull(pager.getStartCursor());
    Assertions.assertNull(pager.getEndCursor());
  }
}
