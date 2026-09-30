package io.spring.application;

import io.spring.application.CursorPager.Direction;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CursorPageParameter<T> {
  public static final int MAX_LIMIT = 100;
  public static final int DEFAULT_LIMIT = 20;
  private int limit = DEFAULT_LIMIT;
  private T cursor;
  private Direction direction;

  public CursorPageParameter(T cursor, int limit, Direction direction) {
    setLimit(limit);
    setCursor(cursor);
    setDirection(direction);
  }

  public boolean isNext() {
    return direction == Direction.NEXT;
  }

  public int getQueryLimit() {
    return limit + 1;
  }

  private void setCursor(T cursor) {
    this.cursor = cursor;
  }

  public static int effectiveLimit(int requested) {
    if (requested > MAX_LIMIT) {
      return MAX_LIMIT;
    }
    return requested > 0 ? requested : DEFAULT_LIMIT;
  }

  private void setLimit(int limit) {
    this.limit = effectiveLimit(limit);
  }
}
