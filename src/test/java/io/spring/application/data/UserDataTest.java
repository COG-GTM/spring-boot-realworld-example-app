package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserDataTest {

  @Test
  public void should_create_user_data_with_all_args_constructor() {
    UserData userData = new UserData("id123", "test@example.com", "testuser", "bio text", "http://image.url");
    
    assertThat(userData.getId(), is("id123"));
    assertThat(userData.getEmail(), is("test@example.com"));
    assertThat(userData.getUsername(), is("testuser"));
    assertThat(userData.getBio(), is("bio text"));
    assertThat(userData.getImage(), is("http://image.url"));
  }

  @Test
  public void should_create_empty_user_data_with_no_args_constructor() {
    UserData userData = new UserData();
    
    assertThat(userData.getId(), nullValue());
    assertThat(userData.getEmail(), nullValue());
    assertThat(userData.getUsername(), nullValue());
    assertThat(userData.getBio(), nullValue());
    assertThat(userData.getImage(), nullValue());
  }

  @Test
  public void should_allow_setting_fields() {
    UserData userData = new UserData();
    userData.setId("id123");
    userData.setEmail("test@example.com");
    userData.setUsername("testuser");
    userData.setBio("bio text");
    userData.setImage("http://image.url");
    
    assertThat(userData.getId(), is("id123"));
    assertThat(userData.getEmail(), is("test@example.com"));
    assertThat(userData.getUsername(), is("testuser"));
    assertThat(userData.getBio(), is("bio text"));
    assertThat(userData.getImage(), is("http://image.url"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    UserData userData1 = new UserData("id1", "email1", "user1", "bio1", "image1");
    UserData userData2 = new UserData("id1", "email1", "user1", "bio1", "image1");
    UserData userData3 = new UserData("id2", "email1", "user1", "bio1", "image1");
    
    assertThat(userData1.equals(userData2), is(true));
    assertThat(userData1.equals(userData3), is(false));
  }

  @Test
  public void should_have_same_hashcode_for_equal_objects() {
    UserData userData1 = new UserData("id1", "email1", "user1", "bio1", "image1");
    UserData userData2 = new UserData("id1", "email1", "user1", "bio1", "image1");
    
    assertThat(userData1.hashCode(), is(userData2.hashCode()));
  }
}
