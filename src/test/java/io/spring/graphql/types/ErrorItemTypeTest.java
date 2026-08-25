package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ErrorItemTypeTest {
  private static final List<String> VALUES = Arrays.asList("can't be empty", "is invalid");

  private static ErrorItem full() {
    return new ErrorItem("email", VALUES);
  }

  @Test
  public void should_build_with_builder() {
    ErrorItem item = ErrorItem.newBuilder().key("email").value(VALUES).build();

    assertThat(item.getKey()).isEqualTo("email");
    assertThat(item.getValue()).containsExactly("can't be empty", "is invalid");
    assertThat(item).isEqualTo(full());
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    ErrorItem item = new ErrorItem();

    assertThat(item.getKey()).isNull();
    assertThat(item.getValue()).isNull();
  }

  @Test
  public void should_apply_setters() {
    ErrorItem item = new ErrorItem();
    item.setKey("email");
    item.setValue(VALUES);

    assertThat(item).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    ErrorItem item = full();

    assertThat(item.getKey()).isEqualTo("email");
    assertThat(item.getValue()).isEqualTo(VALUES);
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    ErrorItem item = full();

    assertThat(item).isEqualTo(item).isEqualTo(full()).isNotEqualTo(null);
    assertThat(item.equals("not an error item")).isFalse();
    assertThat(item.hashCode()).isEqualTo(full().hashCode());
    assertThat(item).isNotEqualTo(new ErrorItem("username", VALUES));
    assertThat(item).isNotEqualTo(new ErrorItem("email", Collections.emptyList()));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("ErrorItem{")
        .contains("key='email'")
        .contains("value='[can't be empty, is invalid]'")
        .endsWith("}");
  }
}
