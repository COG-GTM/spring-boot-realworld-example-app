package io.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_treat_null_as_empty() {
    assertTrue(Util.isEmpty(null));
  }

  @Test
  public void should_treat_empty_string_as_empty() {
    assertTrue(Util.isEmpty(""));
  }

  @Test
  public void should_not_treat_whitespace_as_empty() {
    assertFalse(Util.isEmpty(" "));
  }

  @Test
  public void should_not_treat_text_as_empty() {
    assertFalse(Util.isEmpty("value"));
  }
}
