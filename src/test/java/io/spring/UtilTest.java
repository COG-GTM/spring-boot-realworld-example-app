package io.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_be_empty_for_null() {
    assertTrue(Util.isEmpty(null));
  }

  @Test
  public void should_be_empty_for_empty_string() {
    assertTrue(Util.isEmpty(""));
  }

  @Test
  public void should_not_be_empty_for_blank_string() {
    assertFalse(Util.isEmpty(" "));
  }

  @Test
  public void should_not_be_empty_for_text() {
    assertFalse(Util.isEmpty("value"));
  }
}
