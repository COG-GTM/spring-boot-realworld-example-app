package io.spring.application.pagination;

import io.spring.application.DateTimeCursor;
import io.spring.application.PageCursor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PageCursorTest {

  @Test
  public void should_get_data_from_cursor() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    Assertions.assertEquals(dateTime, cursor.getData());
  }

  @Test
  public void should_convert_to_string() {
    DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(dateTime);

    String result = cursor.toString();
    Assertions.assertNotNull(result);
    Assertions.assertEquals(String.valueOf(dateTime.getMillis()), result);
  }

  @Test
  public void should_handle_different_datetime_values() {
    DateTime dateTime1 = new DateTime(2020, 1, 1, 0, 0, 0, DateTimeZone.UTC);
    DateTime dateTime2 = new DateTime(2025, 12, 31, 23, 59, 59, DateTimeZone.UTC);

    DateTimeCursor cursor1 = new DateTimeCursor(dateTime1);
    DateTimeCursor cursor2 = new DateTimeCursor(dateTime2);

    Assertions.assertNotEquals(cursor1.getData(), cursor2.getData());
    Assertions.assertNotEquals(cursor1.toString(), cursor2.toString());
  }

  @Test
  public void should_preserve_data_integrity() {
    DateTime originalDateTime = new DateTime(2023, 6, 15, 10, 30, 0, DateTimeZone.UTC);
    DateTimeCursor cursor = new DateTimeCursor(originalDateTime);

    DateTime retrievedDateTime = cursor.getData();
    Assertions.assertEquals(originalDateTime.getMillis(), retrievedDateTime.getMillis());
    Assertions.assertEquals(originalDateTime.getYear(), retrievedDateTime.getYear());
    Assertions.assertEquals(originalDateTime.getMonthOfYear(), retrievedDateTime.getMonthOfYear());
    Assertions.assertEquals(originalDateTime.getDayOfMonth(), retrievedDateTime.getDayOfMonth());
  }
}
