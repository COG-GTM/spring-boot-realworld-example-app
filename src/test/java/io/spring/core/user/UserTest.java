package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image.png"));
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "user1", "pass", "", "");
    User user2 = new User("c@d.com", "user2", "pass", "", "");

    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("old@example.com", "user", "pass", "bio", "img");

    user.update("new@example.com", "", "", "", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("user"));
    assertThat(user.getPassword(), is("pass"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("img"));
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("e@e.com", "oldname", "pass", "bio", "img");

    user.update("", "newname", "", "", "");

    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getEmail(), is("e@e.com"));
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("e@e.com", "user", "oldpass", "bio", "img");

    user.update("", "", "newpass", "", "");

    assertThat(user.getPassword(), is("newpass"));
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("e@e.com", "user", "pass", "oldbio", "img");

    user.update("", "", "", "newbio", "");

    assertThat(user.getBio(), is("newbio"));
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("e@e.com", "user", "pass", "bio", "oldimg");

    user.update("", "", "", "", "newimg");

    assertThat(user.getImage(), is("newimg"));
  }

  @Test
  public void should_not_update_fields_when_null() {
    User user = new User("e@e.com", "user", "pass", "bio", "img");

    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("e@e.com"));
    assertThat(user.getUsername(), is("user"));
    assertThat(user.getPassword(), is("pass"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("img"));
  }

  @Test
  public void should_not_update_fields_when_empty_string() {
    User user = new User("e@e.com", "user", "pass", "bio", "img");

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("e@e.com"));
    assertThat(user.getUsername(), is("user"));
    assertThat(user.getPassword(), is("pass"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("img"));
  }

  @Test
  public void should_update_all_fields_at_once() {
    User user = new User("e@e.com", "user", "pass", "bio", "img");

    user.update("new@e.com", "newuser", "newpass", "newbio", "newimg");

    assertThat(user.getEmail(), is("new@e.com"));
    assertThat(user.getUsername(), is("newuser"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimg"));
  }

  @Test
  public void should_have_equality_based_on_id() {
    User user1 = new User("e@e.com", "user", "pass", "bio", "img");
    User user2 = new User("e@e.com", "user", "pass", "bio", "img");

    assertThat(user1.equals(user2), is(false));
    assertThat(user1.equals(user1), is(true));
  }
}
