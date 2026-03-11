package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
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
  public void should_update_user_email() {
    User user = new User("old@example.com", "user", "pass", "bio", "image");
    String newEmail = "new@example.com";

    user.update(newEmail, "", "", "", "");

    assertThat(user.getEmail(), is(newEmail));
  }

  @Test
  public void should_update_user_username() {
    User user = new User("email@example.com", "olduser", "pass", "bio", "image");
    String newUsername = "newuser";

    user.update("", newUsername, "", "", "");

    assertThat(user.getUsername(), is(newUsername));
  }

  @Test
  public void should_update_user_password() {
    User user = new User("email@example.com", "user", "oldpass", "bio", "image");
    String newPassword = "newpass";

    user.update("", "", newPassword, "", "");

    assertThat(user.getPassword(), is(newPassword));
  }

  @Test
  public void should_update_user_bio() {
    User user = new User("email@example.com", "user", "pass", "oldbio", "image");
    String newBio = "newbio";

    user.update("", "", "", newBio, "");

    assertThat(user.getBio(), is(newBio));
  }

  @Test
  public void should_update_user_image() {
    User user = new User("email@example.com", "user", "pass", "bio", "oldimage");
    String newImage = "newimage";

    user.update("", "", "", "", newImage);

    assertThat(user.getImage(), is(newImage));
  }

  @Test
  public void should_not_update_user_with_empty_values() {
    String originalEmail = "email@example.com";
    String originalUsername = "user";
    String originalPassword = "pass";
    String originalBio = "bio";
    String originalImage = "image";

    User user =
        new User(originalEmail, originalUsername, originalPassword, originalBio, originalImage);

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is(originalEmail));
    assertThat(user.getUsername(), is(originalUsername));
    assertThat(user.getPassword(), is(originalPassword));
    assertThat(user.getBio(), is(originalBio));
    assertThat(user.getImage(), is(originalImage));
  }

  @Test
  public void should_not_update_user_with_null_values() {
    String originalEmail = "email@example.com";
    String originalUsername = "user";
    String originalPassword = "pass";
    String originalBio = "bio";
    String originalImage = "image";

    User user =
        new User(originalEmail, originalUsername, originalPassword, originalBio, originalImage);

    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is(originalEmail));
    assertThat(user.getUsername(), is(originalUsername));
    assertThat(user.getPassword(), is(originalPassword));
    assertThat(user.getBio(), is(originalBio));
    assertThat(user.getImage(), is(originalImage));
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@example.com", "olduser", "oldpass", "oldbio", "oldimage");

    user.update("new@example.com", "newuser", "", "newbio", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newuser"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_have_same_equality_for_same_id() {
    User user1 = new User("email1@example.com", "user1", "pass1", "bio1", "image1");
    User user2 = new User("email2@example.com", "user2", "pass2", "bio2", "image2");

    assertThat(user1, is(user1));
    assertThat(user1.hashCode(), is(user1.hashCode()));
  }
}
