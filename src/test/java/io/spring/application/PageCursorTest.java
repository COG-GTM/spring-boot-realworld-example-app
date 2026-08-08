package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PageCursorTest {

  private static class StringCursor extends PageCursor<String> {
    StringCursor(String data) {
      super(data);
    }
  }

  @Test
  public void should_expose_wrapped_data() {
    StringCursor cursor = new StringCursor("abc");

    assertThat(cursor.getData()).isEqualTo("abc");
  }

  @Test
  public void should_delegate_to_string_to_wrapped_data() {
    assertThat(new StringCursor("abc").toString()).isEqualTo("abc");
    assertThat(new PageCursor<Integer>(42) {}.toString()).isEqualTo("42");
  }
}
