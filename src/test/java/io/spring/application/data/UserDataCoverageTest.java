package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserDataCoverageTest {

  @Test
  public void should_create_with_all_args() {
    UserData data = new UserData("id1", "test@test.com", "user", "bio", "img");

    assertEquals("id1", data.getId());
    assertEquals("test@test.com", data.getEmail());
    assertEquals("user", data.getUsername());
    assertEquals("bio", data.getBio());
    assertEquals("img", data.getImage());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    UserData data = new UserData();
    assertNull(data.getId());
    assertNull(data.getEmail());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    UserData data1 = new UserData("id1", "test@test.com", "user", "bio", "img");
    UserData data2 = new UserData("id1", "test@test.com", "user", "bio", "img");

    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void should_have_toString() {
    UserData data = new UserData("id1", "test@test.com", "user", "bio", "img");
    assertNotNull(data.toString());
  }

  @Test
  public void should_set_fields() {
    UserData data = new UserData();
    data.setId("id1");
    data.setEmail("test@test.com");
    data.setUsername("user");
    data.setBio("bio");
    data.setImage("img");

    assertEquals("id1", data.getId());
    assertEquals("test@test.com", data.getEmail());
    assertEquals("user", data.getUsername());
    assertEquals("bio", data.getBio());
    assertEquals("img", data.getImage());
  }
}
