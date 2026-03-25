package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.CursorPager.Direction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CursorPageParameterExtendedTest {

  @Test
  public void should_equals_same_values() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> p1 = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    CursorPageParameter<DateTime> p2 = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
  }

  @Test
  public void should_not_equal_different_values() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> p1 = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    CursorPageParameter<DateTime> p2 = new CursorPageParameter<>(cursor, 20, Direction.PREV);
    assertNotEquals(p1, p2);
  }

  @Test
  public void should_have_toString() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    assertNotNull(p.toString());
  }

  @Test
  public void should_not_equal_null() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(new DateTime(), 10, Direction.NEXT);
    assertNotEquals(null, p);
  }

  @Test
  public void should_not_equal_other_type() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(new DateTime(), 10, Direction.NEXT);
    assertNotEquals("string", p);
  }

  @Test
  public void should_canEqual() {
    CursorPageParameter<DateTime> p1 = new CursorPageParameter<>();
    CursorPageParameter<DateTime> p2 = new CursorPageParameter<>();
    assertTrue(p1.canEqual(p2));
    assertFalse(p1.canEqual("string"));
  }

  @Test
  public void should_get_direction() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(new DateTime(), 10, Direction.NEXT);
    assertEquals(Direction.NEXT, p.getDirection());
  }

  @Test
  public void should_get_cursor() {
    DateTime cursor = new DateTime();
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(cursor, 10, Direction.NEXT);
    assertEquals(cursor, p.getCursor());
  }

  @Test
  public void should_get_limit() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(new DateTime(), 15, Direction.NEXT);
    assertEquals(15, p.getLimit());
  }

  @Test
  public void no_arg_constructor_default_limit() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>();
    assertEquals(20, p.getLimit());
  }

  @Test
  public void should_set_direction() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>();
    p.setDirection(Direction.PREV);
    assertEquals(Direction.PREV, p.getDirection());
  }

  @Test
  public void should_equal_same_object() {
    CursorPageParameter<DateTime> p = new CursorPageParameter<>(new DateTime(), 10, Direction.NEXT);
    assertEquals(p, p);
  }
}
