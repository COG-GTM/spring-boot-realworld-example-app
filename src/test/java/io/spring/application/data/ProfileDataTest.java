package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ProfileDataTest {

  @Test
  public void should_create_profile_data() {
    ProfileData profileData = new ProfileData("id123", "testuser", "bio", "image.jpg", true);
    
    assertThat(profileData.getId(), is("id123"));
    assertThat(profileData.getUsername(), is("testuser"));
    assertThat(profileData.getBio(), is("bio"));
    assertThat(profileData.getImage(), is("image.jpg"));
    assertThat(profileData.isFollowing(), is(true));
  }

  @Test
  public void should_have_equals_and_hashcode() {
    ProfileData profileData1 = new ProfileData("id123", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id123", "testuser", "bio", "image.jpg", true);
    
    assertThat(profileData1.equals(profileData2), is(true));
    assertThat(profileData1.hashCode(), is(profileData2.hashCode()));
  }
}
