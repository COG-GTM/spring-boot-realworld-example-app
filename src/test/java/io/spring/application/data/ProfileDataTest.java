package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ProfileDataTest {

  @Test
  public void should_create_profile_data_with_all_fields() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", true);

    assertEquals("id1", profileData.getId());
    assertEquals("testuser", profileData.getUsername());
    assertEquals("bio", profileData.getBio());
    assertEquals("image.jpg", profileData.getImage());
    assertTrue(profileData.isFollowing());
  }

  @Test
  public void should_create_profile_data_not_following() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", false);

    assertFalse(profileData.isFollowing());
  }

  @Test
  public void should_create_profile_data_with_no_arg_constructor() {
    ProfileData profileData = new ProfileData();
    assertNull(profileData.getId());
    assertNull(profileData.getUsername());
    assertNull(profileData.getBio());
    assertNull(profileData.getImage());
    assertFalse(profileData.isFollowing());
  }

  @Test
  public void should_have_equal_profile_data_with_same_fields() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);

    assertEquals(profileData1, profileData2);
    assertEquals(profileData1.hashCode(), profileData2.hashCode());
  }

  @Test
  public void should_have_unequal_profile_data_with_different_following() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", false);

    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_set_fields_via_setters() {
    ProfileData profileData = new ProfileData();
    profileData.setId("id1");
    profileData.setUsername("testuser");
    profileData.setBio("bio");
    profileData.setImage("image.jpg");
    profileData.setFollowing(true);

    assertEquals("id1", profileData.getId());
    assertEquals("testuser", profileData.getUsername());
    assertEquals("bio", profileData.getBio());
    assertEquals("image.jpg", profileData.getImage());
    assertTrue(profileData.isFollowing());
  }

  @Test
  public void should_not_equal_null() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals(null, profileData);
  }

  @Test
  public void should_not_equal_different_type() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals("not profile data", profileData);
  }

  @Test
  public void should_equal_itself() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertEquals(profileData, profileData);
  }

  @Test
  public void should_have_unequal_profile_data_with_different_id() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id2", "testuser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_different_username() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "otheruser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_different_bio() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "other", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_different_image() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "other.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_toString() {
    ProfileData profileData = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    String str = profileData.toString();
    assertNotNull(str);
    assertTrue(str.contains("testuser"));
  }

  @Test
  public void should_have_unequal_profile_data_with_null_id() {
    ProfileData profileData1 = new ProfileData(null, "testuser", "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_null_username() {
    ProfileData profileData1 = new ProfileData("id1", null, "bio", "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_null_bio() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", null, "image.jpg", true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }

  @Test
  public void should_have_unequal_profile_data_with_null_image() {
    ProfileData profileData1 = new ProfileData("id1", "testuser", "bio", null, true);
    ProfileData profileData2 = new ProfileData("id1", "testuser", "bio", "image.jpg", true);
    assertNotEquals(profileData1, profileData2);
  }
}
