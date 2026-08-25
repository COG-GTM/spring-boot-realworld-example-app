package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTypeTest {
  @Test
  public void should_build_user_with_all_fields() {
    Profile profile = Profile.newBuilder().username("jack").build();

    User user =
        User.newBuilder()
            .email("jack@example.com")
            .profile(profile)
            .token("token")
            .username("jack")
            .build();

    assertThat(user.getEmail()).isEqualTo("jack@example.com");
    assertThat(user.getProfile()).isSameAs(profile);
    assertThat(user.getToken()).isEqualTo("token");
    assertThat(user.getUsername()).isEqualTo("jack");
  }

  @Test
  public void should_construct_user_with_all_args_constructor() {
    Profile profile = Profile.newBuilder().username("jack").build();

    User user = new User("jack@example.com", profile, "token", "jack");

    assertThat(user.getEmail()).isEqualTo("jack@example.com");
    assertThat(user.getProfile()).isSameAs(profile);
    assertThat(user.getToken()).isEqualTo("token");
    assertThat(user.getUsername()).isEqualTo("jack");
  }

  @Test
  public void should_set_fields_with_setters() {
    User user = new User();
    assertThat(user.getEmail()).isNull();

    Profile profile = Profile.newBuilder().username("jill").build();
    user.setEmail("jill@example.com");
    user.setProfile(profile);
    user.setToken("t");
    user.setUsername("jill");

    assertThat(user.getEmail()).isEqualTo("jill@example.com");
    assertThat(user.getProfile()).isSameAs(profile);
    assertThat(user.getToken()).isEqualTo("t");
    assertThat(user.getUsername()).isEqualTo("jill");
  }

  @Test
  public void should_render_fields_in_to_string() {
    User user = new User("jack@example.com", null, "token", "jack");

    assertThat(user.toString())
        .startsWith("User{")
        .contains("email='jack@example.com'")
        .contains("token='token'")
        .contains("username='jack'");
  }

  @Test
  public void should_compare_by_value() {
    User one = new User("jack@example.com", null, "token", "jack");
    User same = new User("jack@example.com", null, "token", "jack");
    User other = new User("jack@example.com", null, "other-token", "jack");

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("nope")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
