package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class PageCursorTest {

  private static class StringCursor extends PageCursor<String> {
    StringCursor(String data) {
      super(data);
    }
  }

  @Test
  public void should_expose_the_wrapped_data() {
    StringCursor cursor = new StringCursor("abc");

    assertThat(cursor.getData()).isEqualTo("abc");
  }

  @Test
  public void should_render_the_wrapped_data_by_default() {
    assertThat(new StringCursor("abc").toString()).isEqualTo("abc");
  }

  @Test
  public void should_allow_null_data_but_fail_to_render_it() {
    StringCursor cursor = new StringCursor(null);

    assertThat(cursor.getData()).isNull();
    assertThatThrownBy(cursor::toString).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void should_let_subclasses_override_the_rendering() {
    DateTime dateTime = new DateTime(42L, DateTimeZone.UTC);

    PageCursor<DateTime> cursor = new DateTimeCursor(dateTime);

    assertThat(cursor.getData()).isEqualTo(dateTime);
    assertThat(cursor.toString()).isEqualTo("42").isNotEqualTo(dateTime.toString());
  }
}
