package io.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_check_empty_values() {
    assertTrue(Util.isEmpty(null));
    assertTrue(Util.isEmpty(""));
    assertFalse(Util.isEmpty("a"));
  }
}
