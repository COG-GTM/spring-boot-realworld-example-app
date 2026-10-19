package io.spring.application;

import org.joda.time.DateTime;

public class CommentCursor extends PageCursor<DateTime> {
  private static final String SEPARATOR = ":";

  private final String id;

  public CommentCursor(DateTime createdAt, String id) {
    super(createdAt);
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return getData().getMillis() + SEPARATOR + id;
  }

  public static CommentCursor parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    int separator = cursor.indexOf(SEPARATOR);
    if (separator < 0) {
      return new CommentCursor(DateTimeCursor.parse(cursor), null);
    }
    return new CommentCursor(
        DateTimeCursor.parse(cursor.substring(0, separator)), cursor.substring(separator + 1));
  }
}
