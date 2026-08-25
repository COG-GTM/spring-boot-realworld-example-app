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
    assertThat(new StringCursor("cursor-value").getData()).isEqualTo("cursor-value");
  }

  @Test
  public void should_use_data_to_string_by_default() {
    assertThat(new StringCursor("cursor-value").toString()).isEqualTo("cursor-value");
  }
}
