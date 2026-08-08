package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UtilTest {
  @Test
  public void should_treat_null_and_empty_string_as_empty() {
    assertThat(Util.isEmpty(null)).isTrue();
    assertThat(Util.isEmpty("")).isTrue();
  }

  @Test
  public void should_treat_whitespace_and_non_empty_string_as_not_empty() {
    assertThat(Util.isEmpty(" ")).isFalse();
    assertThat(Util.isEmpty("value")).isFalse();
  }
}
