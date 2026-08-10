package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UtilTest {

  @Test
  void should_treat_null_as_empty() {
    assertThat(Util.isEmpty(null)).isTrue();
  }

  @Test
  void should_treat_zero_length_string_as_empty() {
    assertThat(Util.isEmpty("")).isTrue();
  }

  @Test
  void should_treat_non_blank_string_as_not_empty() {
    assertThat(Util.isEmpty("jake")).isFalse();
    assertThat(Util.isEmpty(" ")).isFalse();
  }

  @Test
  void should_be_instantiable() {
    assertThat(new Util()).isNotNull();
  }
}
