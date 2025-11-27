package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_return_true_for_null_value() {
    assertThat(Util.isEmpty(null), is(true));
  }

  @Test
  public void should_return_true_for_empty_string() {
    assertThat(Util.isEmpty(""), is(true));
  }

  @Test
  public void should_return_false_for_non_empty_string() {
    assertThat(Util.isEmpty("hello"), is(false));
  }

  @Test
  public void should_return_false_for_whitespace_string() {
    assertThat(Util.isEmpty(" "), is(false));
  }

  @Test
  public void should_return_false_for_string_with_content() {
    assertThat(Util.isEmpty("test value"), is(false));
  }
}
