package io.spring.application;

import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/** Cursor for bookmark lists: bookmark time plus article id, so equal timestamps page stably. */
public class BookmarkCursor extends PageCursor<BookmarkCursor.Position> {
  private static final String SEPARATOR = "_";

  public BookmarkCursor(DateTime bookmarkedAt, String articleId) {
    super(new Position(bookmarkedAt, articleId));
  }

  @Override
  public String toString() {
    return getData().getBookmarkedAt().getMillis() + SEPARATOR + getData().getArticleId();
  }

  public static Position parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    int separator = cursor.indexOf(SEPARATOR);
    String millis = separator < 0 ? cursor : cursor.substring(0, separator);
    String articleId = separator < 0 ? null : cursor.substring(separator + 1);
    return new Position(
        new DateTime().withMillis(Long.parseLong(millis)).withZone(DateTimeZone.UTC), articleId);
  }

  @Value
  public static class Position {
    DateTime bookmarkedAt;
    String articleId;
  }
}
