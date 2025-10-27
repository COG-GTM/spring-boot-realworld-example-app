package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("old@example.com", "username", "password", "bio", "image");
    user.update("new@example.com", "", "", "", "");
    assertThat(user.getEmail(), is("new@example.com"));
  }

  @Test
  public void should_not_update_email_when_empty() {
    User user = new User("old@example.com", "username", "password", "bio", "image");
    user.update("", "", "", "", "");
    assertThat(user.getEmail(), is("old@example.com"));
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("email@example.com", "oldname", "password", "bio", "image");
    user.update("", "newname", "", "", "");
    assertThat(user.getUsername(), is("newname"));
  }

  @Test
  public void should_not_update_username_when_empty() {
    User user = new User("email@example.com", "oldname", "password", "bio", "image");
    user.update("", "", "", "", "");
    assertThat(user.getUsername(), is("oldname"));
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("email@example.com", "username", "oldpass", "bio", "image");
    user.update("", "", "newpass", "", "");
    assertThat(user.getPassword(), is("newpass"));
  }

  @Test
  public void should_not_update_password_when_empty() {
    User user = new User("email@example.com", "username", "oldpass", "bio", "image");
    user.update("", "", "", "", "");
    assertThat(user.getPassword(), is("oldpass"));
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("email@example.com", "username", "password", "oldbio", "image");
    user.update("", "", "", "newbio", "");
    assertThat(user.getBio(), is("newbio"));
  }

  @Test
  public void should_not_update_bio_when_empty() {
    User user = new User("email@example.com", "username", "password", "oldbio", "image");
    user.update("", "", "", "", "");
    assertThat(user.getBio(), is("oldbio"));
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("email@example.com", "username", "password", "bio", "oldimage");
    user.update("", "", "", "", "newimage");
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_not_update_image_when_empty() {
    User user = new User("email@example.com", "username", "password", "bio", "oldimage");
    user.update("", "", "", "", "");
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@example.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("new@example.com", "newname", "newpass", "newbio", "newimage");
    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimage"));
  }
}
