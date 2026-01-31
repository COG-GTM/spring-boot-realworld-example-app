package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class ProfileDataTest {

  @Test
  public void should_create_profile_data_with_all_fields() {
    ProfileData profileData =
        new ProfileData("user-id", "testuser", "Test bio", "http://image.url", true);

    assertThat(profileData.getId(), is("user-id"));
    assertThat(profileData.getUsername(), is("testuser"));
    assertThat(profileData.getBio(), is("Test bio"));
    assertThat(profileData.getImage(), is("http://image.url"));
    assertThat(profileData.isFollowing(), is(true));
  }

  @Test
  public void should_create_empty_profile_data() {
    ProfileData profileData = new ProfileData();

    assertThat(profileData.getId(), is((String) null));
    assertThat(profileData.getUsername(), is((String) null));
    assertThat(profileData.getBio(), is((String) null));
    assertThat(profileData.getImage(), is((String) null));
    assertThat(profileData.isFollowing(), is(false));
  }

  @Test
  public void should_set_profile_data_fields() {
    ProfileData profileData = new ProfileData();
    profileData.setId("new-id");
    profileData.setUsername("newuser");
    profileData.setBio("New bio");
    profileData.setImage("http://new-image.url");
    profileData.setFollowing(true);

    assertThat(profileData.getId(), is("new-id"));
    assertThat(profileData.getUsername(), is("newuser"));
    assertThat(profileData.getBio(), is("New bio"));
    assertThat(profileData.getImage(), is("http://new-image.url"));
    assertThat(profileData.isFollowing(), is(true));
  }

  @Test
  public void should_implement_equals_and_hashcode() {
    ProfileData profileData1 =
        new ProfileData("user-id", "testuser", "Test bio", "http://image.url", true);
    ProfileData profileData2 =
        new ProfileData("user-id", "testuser", "Test bio", "http://image.url", true);
    ProfileData profileData3 =
        new ProfileData("other-id", "otheruser", "Other bio", "http://other.url", false);

    assertEquals(profileData1, profileData2);
    assertNotEquals(profileData1, profileData3);
    assertEquals(profileData1.hashCode(), profileData2.hashCode());
    assertThat(profileData1.hashCode(), not(profileData3.hashCode()));
  }

  @Test
  public void should_implement_toString() {
    ProfileData profileData =
        new ProfileData("user-id", "testuser", "Test bio", "http://image.url", true);

    String toString = profileData.toString();
    assertThat(toString.contains("testuser"), is(true));
  }
}
