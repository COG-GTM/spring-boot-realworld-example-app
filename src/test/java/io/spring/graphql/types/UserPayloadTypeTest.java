package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserPayloadTypeTest {
  private static User sample() {
    return User.newBuilder().email("jack@example.com").username("jack").token("token").build();
  }

  @Test
  public void should_build_with_builder() {
    User value = sample();
    UserPayload payload = UserPayload.newBuilder().user(value).build();
    assertThat(payload.getUser()).isSameAs(value);
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    User value = sample();
    UserPayload payload = new UserPayload(value);
    assertThat(payload.getUser()).isSameAs(value);
  }

  @Test
  public void should_support_no_args_constructor_and_setter() {
    UserPayload payload = new UserPayload();
    assertThat(payload.getUser()).isNull();
    User value = sample();
    payload.setUser(value);
    assertThat(payload.getUser()).isSameAs(value);
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    UserPayload one = new UserPayload(sample());
    UserPayload same = new UserPayload(sample());
    UserPayload different = new UserPayload(User.newBuilder().email("john@example.com").build());
    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(different).isNotEqualTo(null);
    assertThat(one.equals("not a payload")).isFalse();
    assertThat(one.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_render_field_in_to_string() {
    UserPayload payload = new UserPayload(sample());
    assertThat(payload.toString())
        .startsWith("UserPayload{")
        .contains("jack@example.com")
        .endsWith("}");
  }
}
