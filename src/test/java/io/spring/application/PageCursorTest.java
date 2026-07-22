package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PageCursorTest {

  private static class StringCursor extends PageCursor<String> {
    StringCursor(String data) {
      super(data);
    }
  }

  @Test
  public void should_expose_data() {
    StringCursor cursor = new StringCursor("value");
    assertThat(cursor.getData(), is("value"));
  }

  @Test
  public void should_use_data_to_string_by_default() {
    StringCursor cursor = new StringCursor("value");
    assertThat(cursor.toString(), is("value"));
  }
}
