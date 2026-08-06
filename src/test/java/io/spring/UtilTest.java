package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_be_empty_for_null() {
    assertThat(Util.isEmpty(null)).isTrue();
  }

  @Test
  public void should_be_empty_for_empty_string() {
    assertThat(Util.isEmpty("")).isTrue();
  }

  @Test
  public void should_not_be_empty_for_blank_string() {
    assertThat(Util.isEmpty(" ")).isFalse();
  }

  @Test
  public void should_not_be_empty_for_non_empty_string() {
    assertThat(Util.isEmpty("value")).isFalse();
  }
}
