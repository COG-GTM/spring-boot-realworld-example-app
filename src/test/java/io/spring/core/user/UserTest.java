package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    
    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image.jpg"));
  }

  @Test
  public void should_update_email() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("newemail@example.com", "", "", "", "");
    
    assertThat(user.getEmail(), is("newemail@example.com"));
    assertThat(user.getUsername(), is("testuser"));
  }

  @Test
  public void should_update_username() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("", "newusername", "", "", "");
    
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("newusername"));
  }

  @Test
  public void should_update_password() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("", "", "newpassword", "", "");
    
    assertThat(user.getPassword(), is("newpassword"));
  }

  @Test
  public void should_update_bio() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("", "", "", "new bio", "");
    
    assertThat(user.getBio(), is("new bio"));
  }

  @Test
  public void should_update_image() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("", "", "", "", "newimage.jpg");
    
    assertThat(user.getImage(), is("newimage.jpg"));
  }

  @Test
  public void should_update_multiple_fields() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("newemail@example.com", "newusername", "newpassword", "new bio", "newimage.jpg");
    
    assertThat(user.getEmail(), is("newemail@example.com"));
    assertThat(user.getUsername(), is("newusername"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("newimage.jpg"));
  }

  @Test
  public void should_not_update_with_null_values() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update(null, null, null, null, null);
    
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image.jpg"));
  }

  @Test
  public void should_have_equals_based_on_id() {
    User user1 = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    User user2 = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    
    assertThat(user1.equals(user2), is(false));
    assertThat(user1.equals(user1), is(true));
  }
}
