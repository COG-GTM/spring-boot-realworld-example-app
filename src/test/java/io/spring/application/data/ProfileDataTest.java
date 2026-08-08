package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ProfileDataTest {

  @Test
  public void should_expose_all_constructor_values() {
    ProfileData profileData = new ProfileData("user-id", "jane", "bio", "image", true);

    assertThat(profileData.getId()).isEqualTo("user-id");
    assertThat(profileData.getUsername()).isEqualTo("jane");
    assertThat(profileData.getBio()).isEqualTo("bio");
    assertThat(profileData.getImage()).isEqualTo("image");
    assertThat(profileData.isFollowing()).isTrue();
  }

  @Test
  public void should_default_following_to_false_and_apply_setters() {
    ProfileData profileData = new ProfileData();

    assertThat(profileData.isFollowing()).isFalse();
    assertThat(profileData.getUsername()).isNull();

    profileData.setUsername("john");
    profileData.setFollowing(true);

    assertThat(profileData.getUsername()).isEqualTo("john");
    assertThat(profileData.isFollowing()).isTrue();
  }

  @Test
  public void should_distinguish_profiles_by_following_flag() {
    ProfileData followed = new ProfileData("user-id", "jane", "bio", "image", true);
    ProfileData notFollowed = new ProfileData("user-id", "jane", "bio", "image", false);

    assertThat(followed).isNotEqualTo(notFollowed);
    assertThat(followed).isEqualTo(new ProfileData("user-id", "jane", "bio", "image", true));
    assertThat(followed.toString()).contains("jane");
  }
}
