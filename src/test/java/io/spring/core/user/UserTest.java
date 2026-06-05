package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password123", "my bio", "image.jpg");
    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("test@example.com"));
    assertThat(user.getUsername(), is("testuser"));
    assertThat(user.getPassword(), is("password123"));
    assertThat(user.getBio(), is("my bio"));
    assertThat(user.getImage(), is("image.jpg"));
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "user1", "pass", "", "");
    User user2 = new User("c@d.com", "user2", "pass", "", "");
    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_email() {
    User user = new User("old@test.com", "username", "pass", "bio", "img");
    user.update("new@test.com", null, null, null, null);
    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("username"));
  }

  @Test
  public void should_update_username() {
    User user = new User("test@test.com", "oldname", "pass", "bio", "img");
    user.update(null, "newname", null, null, null);
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getEmail(), is("test@test.com"));
  }

  @Test
  public void should_update_password() {
    User user = new User("test@test.com", "name", "oldpass", "bio", "img");
    user.update(null, null, "newpass", null, null);
    assertThat(user.getPassword(), is("newpass"));
  }

  @Test
  public void should_update_bio() {
    User user = new User("test@test.com", "name", "pass", "oldbio", "img");
    user.update(null, null, null, "newbio", null);
    assertThat(user.getBio(), is("newbio"));
  }

  @Test
  public void should_update_image() {
    User user = new User("test@test.com", "name", "pass", "bio", "oldimg");
    user.update(null, null, null, null, "newimg");
    assertThat(user.getImage(), is("newimg"));
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@test.com", "oldname", "oldpass", "oldbio", "oldimg");
    user.update("new@test.com", "newname", "newpass", "newbio", "newimg");
    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimg"));
  }

  @Test
  public void should_not_update_fields_with_empty_string() {
    User user = new User("test@test.com", "name", "pass", "bio", "img");
    user.update("", "", "", "", "");
    assertThat(user.getEmail(), is("test@test.com"));
    assertThat(user.getUsername(), is("name"));
    assertThat(user.getPassword(), is("pass"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("img"));
  }

  @Test
  public void should_not_update_fields_with_null() {
    User user = new User("test@test.com", "name", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertThat(user.getEmail(), is("test@test.com"));
    assertThat(user.getUsername(), is("name"));
    assertThat(user.getPassword(), is("pass"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("img"));
  }

  @Test
  public void should_have_equality_based_on_id() {
    User user1 = new User("a@b.com", "user1", "pass", "", "");
    User user2 = new User("c@d.com", "user2", "pass", "", "");
    assertThat(user1.equals(user2), is(false));
    assertThat(user1.equals(user1), is(true));
  }
}
