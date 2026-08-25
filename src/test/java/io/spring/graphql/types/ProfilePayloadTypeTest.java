package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ProfilePayloadTypeTest {
  private static Profile sample() {
    return Profile.newBuilder().username("jack").bio("bio").following(true).build();
  }

  @Test
  public void should_build_with_builder() {
    Profile value = sample();
    ProfilePayload payload = ProfilePayload.newBuilder().profile(value).build();
    assertThat(payload.getProfile()).isSameAs(value);
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    Profile value = sample();
    ProfilePayload payload = new ProfilePayload(value);
    assertThat(payload.getProfile()).isSameAs(value);
  }

  @Test
  public void should_support_no_args_constructor_and_setter() {
    ProfilePayload payload = new ProfilePayload();
    assertThat(payload.getProfile()).isNull();
    Profile value = sample();
    payload.setProfile(value);
    assertThat(payload.getProfile()).isSameAs(value);
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    ProfilePayload one = new ProfilePayload(sample());
    ProfilePayload same = new ProfilePayload(sample());
    ProfilePayload different = new ProfilePayload(Profile.newBuilder().username("john").build());
    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(different).isNotEqualTo(null);
    assertThat(one.equals("not a payload")).isFalse();
    assertThat(one.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_render_field_in_to_string() {
    ProfilePayload payload = new ProfilePayload(sample());
    assertThat(payload.toString()).startsWith("ProfilePayload{").contains("jack").endsWith("}");
  }
}
