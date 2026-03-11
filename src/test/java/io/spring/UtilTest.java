package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_return_true_for_null_value() {
    assertTrue(Util.isEmpty(null));
  }

  @Test
  public void should_return_true_for_empty_string() {
    assertTrue(Util.isEmpty(""));
  }

  @Test
  public void should_return_false_for_non_empty_string() {
    assertFalse(Util.isEmpty("hello"));
  }

  @Test
  public void should_return_false_for_whitespace_only_string() {
    assertFalse(Util.isEmpty("   "));
  }

  @Test
  public void should_return_false_for_single_character() {
    assertFalse(Util.isEmpty("a"));
  }

  @Test
  public void should_return_false_for_string_with_spaces() {
    assertFalse(Util.isEmpty("hello world"));
  }
}
