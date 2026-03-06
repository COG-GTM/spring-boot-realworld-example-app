package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserDataTest {

  @Test
  public void should_create_user_data_with_all_fields() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertEquals("id1", userData.getId());
    assertEquals("test@example.com", userData.getEmail());
    assertEquals("testuser", userData.getUsername());
    assertEquals("my bio", userData.getBio());
    assertEquals("image.jpg", userData.getImage());
  }

  @Test
  public void should_create_user_data_with_no_arg_constructor() {
    UserData userData = new UserData();
    assertNull(userData.getId());
    assertNull(userData.getEmail());
    assertNull(userData.getUsername());
    assertNull(userData.getBio());
    assertNull(userData.getImage());
  }

  @Test
  public void should_set_fields_via_setters() {
    UserData userData = new UserData();
    userData.setId("id1");
    userData.setEmail("test@example.com");
    userData.setUsername("testuser");
    userData.setBio("my bio");
    userData.setImage("image.jpg");

    assertEquals("id1", userData.getId());
    assertEquals("test@example.com", userData.getEmail());
    assertEquals("testuser", userData.getUsername());
    assertEquals("my bio", userData.getBio());
    assertEquals("image.jpg", userData.getImage());
  }

  @Test
  public void should_have_equal_user_data_with_same_fields() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertEquals(userData1, userData2);
    assertEquals(userData1.hashCode(), userData2.hashCode());
  }

  @Test
  public void should_have_unequal_user_data_with_different_fields() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 =
        new UserData("id2", "other@example.com", "otheruser", "other bio", "other.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_different_id() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 = new UserData("id2", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_different_email() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 =
        new UserData("id1", "other@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_different_username() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 =
        new UserData("id1", "test@example.com", "otheruser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_different_bio() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 =
        new UserData("id1", "test@example.com", "testuser", "other bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_different_image() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "other.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_not_equal_null() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    assertNotEquals(null, userData);
  }

  @Test
  public void should_not_equal_different_type() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    assertNotEquals("not user data", userData);
  }

  @Test
  public void should_have_toString() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    String str = userData.toString();
    assertNotNull(str);
    assertTrue(str.contains("test@example.com"));
    assertTrue(str.contains("testuser"));
  }

  @Test
  public void should_equal_itself() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");
    assertEquals(userData, userData);
  }

  @Test
  public void should_have_unequal_user_data_with_null_id() {
    UserData userData1 = new UserData(null, "test@example.com", "testuser", "my bio", "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_null_email() {
    UserData userData1 = new UserData("id1", null, "testuser", "my bio", "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_null_username() {
    UserData userData1 = new UserData("id1", "test@example.com", null, "my bio", "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_null_bio() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", null, "image.jpg");
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }

  @Test
  public void should_have_unequal_user_data_with_null_image() {
    UserData userData1 = new UserData("id1", "test@example.com", "testuser", "my bio", null);
    UserData userData2 = new UserData("id1", "test@example.com", "testuser", "my bio", "image.jpg");

    assertNotEquals(userData1, userData2);
  }
}
