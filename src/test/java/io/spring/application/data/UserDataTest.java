package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserDataTest {

  @Test
  public void should_create_user_data() {
    UserData userData = new UserData("id123", "test@example.com", "testuser", "bio", "image.jpg");
    
    assertThat(userData.getId(), is("id123"));
    assertThat(userData.getEmail(), is("test@example.com"));
    assertThat(userData.getUsername(), is("testuser"));
    assertThat(userData.getBio(), is("bio"));
    assertThat(userData.getImage(), is("image.jpg"));
  }

  @Test
  public void should_have_equals_and_hashcode() {
    UserData userData1 = new UserData("id123", "test@example.com", "testuser", "bio", "image.jpg");
    UserData userData2 = new UserData("id123", "test@example.com", "testuser", "bio", "image.jpg");
    
    assertThat(userData1.equals(userData2), is(true));
    assertThat(userData1.hashCode(), is(userData2.hashCode()));
  }
}
