package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@email.com", "username", "password", "bio", "image");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_create_user_with_empty_bio_and_image() {
    User user = new User("test@email.com", "username", "password", "", "");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is(""));
    assertThat(user.getImage(), is(""));
  }

  @Test
  public void should_generate_unique_id_for_each_user() {
    User user1 = new User("test1@email.com", "username1", "password1", "", "");
    User user2 = new User("test2@email.com", "username2", "password2", "", "");

    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update("new@email.com", null, null, null, null);

    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update(null, "newusername", null, null, null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("newusername"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update(null, null, "newpassword", null, null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update(null, null, null, "newbio", null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update(null, null, null, null, "newimage");

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_update_all_fields_at_once() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update("new@email.com", "newusername", "newpassword", "newbio", "newimage");

    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("newusername"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_not_update_fields_when_empty_string() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_not_update_fields_when_null() {
    User user = new User("test@email.com", "username", "password", "bio", "image");
    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("test@email.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_have_equal_users_with_same_id() {
    User user1 = new User("test@email.com", "username", "password", "bio", "image");
    User user2 = new User("other@email.com", "other", "other", "other", "other");

    assertThat(user1.equals(user1), is(true));
    assertThat(user1.equals(user2), is(false));
  }
}
