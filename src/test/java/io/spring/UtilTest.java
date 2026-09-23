package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_be_empty_when_value_is_null() {
    assertThat(Util.isEmpty(null), is(true));
  }

  @Test
  public void should_be_empty_when_value_has_no_character() {
    assertThat(Util.isEmpty(""), is(true));
  }

  @Test
  public void should_not_be_empty_when_value_only_contains_whitespace() {
    assertThat(Util.isEmpty(" "), is(false));
  }

  @Test
  public void should_not_be_empty_when_value_has_content() {
    assertThat(Util.isEmpty("content"), is(false));
  }
}
