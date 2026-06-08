package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  void isEmpty_should_return_true_for_null() {
    assertTrue(Util.isEmpty(null));
  }

  @Test
  void isEmpty_should_return_true_for_empty_string() {
    assertTrue(Util.isEmpty(""));
  }

  @Test
  void isEmpty_should_return_false_for_non_empty_string() {
    assertFalse(Util.isEmpty("hello"));
  }
}
