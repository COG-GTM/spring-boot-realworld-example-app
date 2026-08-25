package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DeletionStatusTypeTest {
  @Test
  public void should_build_with_builder() {
    DeletionStatus status = DeletionStatus.newBuilder().success(true).build();

    assertThat(status.getSuccess()).isTrue();
    assertThat(status).isEqualTo(new DeletionStatus(true));
  }

  @Test
  public void should_default_success_to_false_with_no_args_constructor() {
    DeletionStatus status = new DeletionStatus();

    assertThat(status.getSuccess()).isFalse();
  }

  @Test
  public void should_apply_setter() {
    DeletionStatus status = new DeletionStatus();
    status.setSuccess(true);

    assertThat(status.getSuccess()).isTrue();
    status.setSuccess(false);
    assertThat(status.getSuccess()).isFalse();
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    assertThat(new DeletionStatus(true).getSuccess()).isTrue();
    assertThat(new DeletionStatus(false).getSuccess()).isFalse();
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    DeletionStatus status = new DeletionStatus(true);

    assertThat(status).isEqualTo(status).isEqualTo(new DeletionStatus(true)).isNotEqualTo(null);
    assertThat(status).isNotEqualTo(new DeletionStatus(false));
    assertThat(status.equals("not a status")).isFalse();
    assertThat(status.hashCode()).isEqualTo(new DeletionStatus(true).hashCode());
  }

  @Test
  public void should_render_success_in_to_string() {
    assertThat(new DeletionStatus(true).toString()).isEqualTo("DeletionStatus{success='true'}");
    assertThat(new DeletionStatus(false).toString()).isEqualTo("DeletionStatus{success='false'}");
  }
}
