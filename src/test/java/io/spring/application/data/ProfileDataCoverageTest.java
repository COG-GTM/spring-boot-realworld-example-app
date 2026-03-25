package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ProfileDataCoverageTest {

  @Test
  public void should_create_with_all_args() {
    ProfileData data = new ProfileData("id1", "user", "bio", "img", true);

    assertEquals("id1", data.getId());
    assertEquals("user", data.getUsername());
    assertEquals("bio", data.getBio());
    assertEquals("img", data.getImage());
    assertTrue(data.isFollowing());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    ProfileData data = new ProfileData();
    assertNull(data.getId());
    assertNull(data.getUsername());
    assertFalse(data.isFollowing());
  }

  @Test
  public void should_set_following() {
    ProfileData data = new ProfileData("id1", "user", "bio", "img", false);
    assertFalse(data.isFollowing());
    data.setFollowing(true);
    assertTrue(data.isFollowing());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    ProfileData data1 = new ProfileData("id1", "user", "bio", "img", false);
    ProfileData data2 = new ProfileData("id1", "user", "bio", "img", false);

    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void should_not_equal_different() {
    ProfileData data1 = new ProfileData("id1", "user1", "bio", "img", false);
    ProfileData data2 = new ProfileData("id2", "user2", "bio", "img", false);

    assertNotEquals(data1, data2);
  }

  @Test
  public void should_have_toString() {
    ProfileData data = new ProfileData("id1", "user", "bio", "img", false);
    assertNotNull(data.toString());
    assertTrue(data.toString().contains("user"));
  }

  @Test
  public void should_set_all_fields() {
    ProfileData data = new ProfileData();
    data.setId("id1");
    data.setUsername("user");
    data.setBio("bio");
    data.setImage("img");
    data.setFollowing(true);

    assertEquals("id1", data.getId());
    assertEquals("user", data.getUsername());
    assertEquals("bio", data.getBio());
    assertEquals("img", data.getImage());
    assertTrue(data.isFollowing());
  }
}
