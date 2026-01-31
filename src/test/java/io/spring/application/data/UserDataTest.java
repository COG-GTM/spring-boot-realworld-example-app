package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class UserDataTest {

  @Test
  public void should_create_user_data_with_all_fields() {
    UserData userData =
        new UserData("user-id", "test@example.com", "testuser", "Test bio", "http://image.url");

    assertThat(userData.getId(), is("user-id"));
    assertThat(userData.getEmail(), is("test@example.com"));
    assertThat(userData.getUsername(), is("testuser"));
    assertThat(userData.getBio(), is("Test bio"));
    assertThat(userData.getImage(), is("http://image.url"));
  }

  @Test
  public void should_create_empty_user_data() {
    UserData userData = new UserData();

    assertThat(userData.getId(), is((String) null));
    assertThat(userData.getEmail(), is((String) null));
    assertThat(userData.getUsername(), is((String) null));
    assertThat(userData.getBio(), is((String) null));
    assertThat(userData.getImage(), is((String) null));
  }

  @Test
  public void should_set_user_data_fields() {
    UserData userData = new UserData();
    userData.setId("new-id");
    userData.setEmail("new@example.com");
    userData.setUsername("newuser");
    userData.setBio("New bio");
    userData.setImage("http://new-image.url");

    assertThat(userData.getId(), is("new-id"));
    assertThat(userData.getEmail(), is("new@example.com"));
    assertThat(userData.getUsername(), is("newuser"));
    assertThat(userData.getBio(), is("New bio"));
    assertThat(userData.getImage(), is("http://new-image.url"));
  }

  @Test
  public void should_implement_equals_and_hashcode() {
    UserData userData1 =
        new UserData("user-id", "test@example.com", "testuser", "Test bio", "http://image.url");
    UserData userData2 =
        new UserData("user-id", "test@example.com", "testuser", "Test bio", "http://image.url");
    UserData userData3 =
        new UserData("other-id", "other@example.com", "otheruser", "Other bio", "http://other.url");

    assertEquals(userData1, userData2);
    assertNotEquals(userData1, userData3);
    assertEquals(userData1.hashCode(), userData2.hashCode());
    assertThat(userData1.hashCode(), not(userData3.hashCode()));
  }

  @Test
  public void should_implement_toString() {
    UserData userData =
        new UserData("user-id", "test@example.com", "testuser", "Test bio", "http://image.url");

    String toString = userData.toString();
    assertThat(toString.contains("user-id"), is(true));
    assertThat(toString.contains("test@example.com"), is(true));
    assertThat(toString.contains("testuser"), is(true));
  }
}
