package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_correct_fields() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    assertThat(user.getId(), notNullValue());
    assertThat(user.getId().length(), is(36));
    assertThat(user.getEmail(), is("user@example.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "u1", "p1", "", "");
    User user2 = new User("c@d.com", "u2", "p2", "", "");
    assertThat(user1.getId(), not(user2.getId()));
  }

  @Test
  public void should_update_non_empty_fields() {
    User user = new User("old@email.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("new@email.com", "newname", "newpass", "newbio", "newimage");
    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_not_update_empty_fields() {
    User user = new User("old@email.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("", "", "", "", "");
    assertThat(user.getEmail(), is("old@email.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("oldbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_not_update_null_fields() {
    User user = new User("old@email.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update(null, null, null, null, null);
    assertThat(user.getEmail(), is("old@email.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("oldbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_update_partial_fields() {
    User user = new User("old@email.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("new@email.com", "", null, "newbio", "");
    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_be_equal_when_same_id() {
    User user1 = new User("a@b.com", "u1", "p1", "b1", "i1");
    User user2 = new User("c@d.com", "u2", "p2", "b2", "i2");
    assertThat(user1, not(user2));
    assertThat(user1, is(user1));
    assertThat(user1.hashCode(), is(user1.hashCode()));
  }
}
