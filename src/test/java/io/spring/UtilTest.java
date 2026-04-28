package io.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_return_true_for_null() {
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
  public void should_return_false_for_whitespace_string() {
    assertFalse(Util.isEmpty(" "));
  }
}
