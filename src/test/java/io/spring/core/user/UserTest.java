package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@email.com", "testuser", "password123", "my bio", "http://image.url");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("my bio"));
    assertThat(user.getImage(), is("http://image.url"));
  }

  @Test
  public void should_create_user_with_empty_bio_and_image() {
    User user = new User("test@email.com", "testuser", "password123", "", "");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is(""));
    assertThat(user.getImage(), is(""));
  }

  @Test
  public void should_generate_unique_id_for_each_user() {
    User user1 = new User("test1@email.com", "testuser1", "password123", "", "");
    User user2 = new User("test2@email.com", "testuser2", "password456", "", "");

    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("old@email.com", "testuser", "password123", "bio", "image");

    user.update("new@email.com", null, null, null, null);

    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("test@email.com", "olduser", "password123", "bio", "image");

    user.update(null, "newuser", null, null, null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("newuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("test@email.com", "testuser", "oldpassword", "bio", "image");

    user.update(null, null, "newpassword", null, null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("test@email.com", "testuser", "password123", "old bio", "image");

    user.update(null, null, null, "new bio", null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "old-image.jpg");

    user.update(null, null, null, null, "new-image.jpg");

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("new-image.jpg"));
  }

  @Test
  public void should_update_all_fields_at_once() {
    User user = new User("old@email.com", "olduser", "oldpassword", "old bio", "old-image.jpg");

    user.update("new@email.com", "newuser", "newpassword", "new bio", "new-image.jpg");

    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("newuser"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new-image.jpg"));
  }

  @Test
  public void should_not_update_email_when_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    user.update("", null, null, null, null);

    assertThat(user.getEmail(), is("test@email.com"));
  }

  @Test
  public void should_not_update_username_when_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    user.update(null, "", null, null, null);

    assertThat(user.getUsername(), is("testuser"));
  }

  @Test
  public void should_not_update_password_when_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    user.update(null, null, "", null, null);

    assertThat(user.getPassword(), is("password123"));
  }

  @Test
  public void should_not_update_bio_when_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    user.update(null, null, null, "", null);

    assertThat(user.getBio(), is("bio"));
  }

  @Test
  public void should_not_update_image_when_empty() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    user.update(null, null, null, null, "");

    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_have_equality_based_on_id() {
    User user1 = new User("test@email.com", "testuser", "password123", "bio", "image");
    User user2 = new User("test@email.com", "testuser", "password123", "bio", "image");

    assertThat(user1.equals(user2), is(false));
    assertThat(user1.equals(user1), is(true));
  }

  @Test
  public void should_have_consistent_hashcode_based_on_id() {
    User user = new User("test@email.com", "testuser", "password123", "bio", "image");

    int hashCode1 = user.hashCode();
    int hashCode2 = user.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }
}
