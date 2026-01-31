package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    String email = "test@example.com";
    String username = "testuser";
    String password = "password123";
    String bio = "Test bio";
    String image = "http://example.com/image.jpg";

    User user = new User(email, username, password, bio, image);

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is(email));
    assertThat(user.getUsername(), is(username));
    assertThat(user.getPassword(), is(password));
    assertThat(user.getBio(), is(bio));
    assertThat(user.getImage(), is(image));
  }

  @Test
  public void should_generate_unique_id_for_each_user() {
    User user1 = new User("user1@example.com", "user1", "pass1", "", "");
    User user2 = new User("user2@example.com", "user2", "pass2", "", "");

    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("old@example.com", "username", "password", "bio", "image");
    String originalEmail = user.getEmail();

    user.update("new@example.com", "", "", "", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getEmail(), not(originalEmail));
  }

  @Test
  public void should_not_update_email_when_empty() {
    User user = new User("original@example.com", "username", "password", "bio", "image");
    String originalEmail = user.getEmail();

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is(originalEmail));
  }

  @Test
  public void should_not_update_email_when_null() {
    User user = new User("original@example.com", "username", "password", "bio", "image");
    String originalEmail = user.getEmail();

    user.update(null, "", "", "", "");

    assertThat(user.getEmail(), is(originalEmail));
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("email@example.com", "oldusername", "password", "bio", "image");

    user.update("", "newusername", "", "", "");

    assertThat(user.getUsername(), is("newusername"));
  }

  @Test
  public void should_not_update_username_when_empty() {
    User user = new User("email@example.com", "originalusername", "password", "bio", "image");
    String originalUsername = user.getUsername();

    user.update("", "", "", "", "");

    assertThat(user.getUsername(), is(originalUsername));
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("email@example.com", "username", "oldpassword", "bio", "image");

    user.update("", "", "newpassword", "", "");

    assertThat(user.getPassword(), is("newpassword"));
  }

  @Test
  public void should_not_update_password_when_empty() {
    User user = new User("email@example.com", "username", "originalpassword", "bio", "image");
    String originalPassword = user.getPassword();

    user.update("", "", "", "", "");

    assertThat(user.getPassword(), is(originalPassword));
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("email@example.com", "username", "password", "old bio", "image");

    user.update("", "", "", "new bio", "");

    assertThat(user.getBio(), is("new bio"));
  }

  @Test
  public void should_not_update_bio_when_empty() {
    User user = new User("email@example.com", "username", "password", "original bio", "image");
    String originalBio = user.getBio();

    user.update("", "", "", "", "");

    assertThat(user.getBio(), is(originalBio));
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("email@example.com", "username", "password", "bio", "old-image.jpg");

    user.update("", "", "", "", "new-image.jpg");

    assertThat(user.getImage(), is("new-image.jpg"));
  }

  @Test
  public void should_not_update_image_when_empty() {
    User user = new User("email@example.com", "username", "password", "bio", "original-image.jpg");
    String originalImage = user.getImage();

    user.update("", "", "", "", "");

    assertThat(user.getImage(), is(originalImage));
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@example.com", "olduser", "oldpass", "old bio", "old.jpg");

    user.update("new@example.com", "newuser", "newpass", "new bio", "new.jpg");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newuser"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new.jpg"));
  }

  @Test
  public void should_update_only_non_empty_fields() {
    User user = new User("original@example.com", "originaluser", "originalpass", "original bio", "original.jpg");

    user.update("new@example.com", "", "newpass", "", "new.jpg");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("originaluser"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("original bio"));
    assertThat(user.getImage(), is("new.jpg"));
  }

  @Test
  public void should_have_equal_users_with_same_id() {
    User user1 = new User("email@example.com", "username", "password", "bio", "image");
    User user2 = user1;

    assertThat(user1.equals(user2), is(true));
    assertThat(user1.hashCode(), is(user2.hashCode()));
  }

  @Test
  public void should_have_different_users_with_different_ids() {
    User user1 = new User("email1@example.com", "username1", "password1", "bio1", "image1");
    User user2 = new User("email2@example.com", "username2", "password2", "bio2", "image2");

    assertThat(user1.equals(user2), is(false));
  }
}
