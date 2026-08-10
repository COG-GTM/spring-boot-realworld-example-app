package io.spring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_return_true_for_null() {
    Assertions.assertTrue(Util.isEmpty(null));
  }

  @Test
  public void should_return_true_for_empty_string() {
    Assertions.assertTrue(Util.isEmpty(""));
  }

  @Test
  public void should_return_false_for_non_empty_string() {
    Assertions.assertFalse(Util.isEmpty("content"));
  }
}
