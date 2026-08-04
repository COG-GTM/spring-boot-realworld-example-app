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
  public void should_hold_the_wrapped_data() {
    StringCursor cursor = new StringCursor("abc");

    assertThat(cursor.getData(), is("abc"));
  }

  @Test
  public void should_serialize_data_with_to_string() {
    assertThat(new StringCursor("abc").toString(), is("abc"));
  }

  @Test
  public void should_round_trip_through_string_representation() {
    StringCursor cursor = new StringCursor("some-cursor-value");

    StringCursor parsed = new StringCursor(cursor.toString());

    assertThat(parsed.getData(), is(cursor.getData()));
  }
}
