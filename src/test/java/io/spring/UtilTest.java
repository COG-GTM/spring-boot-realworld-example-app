package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UtilTest {

  @Test
  public void should_identify_empty_values() {
    assertThat(Util.isEmpty(null), is(true));
    assertThat(Util.isEmpty(""), is(true));
    assertThat(Util.isEmpty("a"), is(false));
    assertThat(Util.isEmpty(" "), is(false));
  }
}
