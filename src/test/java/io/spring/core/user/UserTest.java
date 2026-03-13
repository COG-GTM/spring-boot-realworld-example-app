package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_set_all_fields_via_constructor() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("user@example.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "u1", "p1", "bio1", "img1");
    User user2 = new User("c@d.com", "u2", "p2", "bio2", "img2");
    assertThat(user1.getId(), is(not(user2.getId())));
  }

  @Test
  public void should_update_fields_when_non_empty_values_passed() {
    User user = new User("old@example.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("new@example.com", "newname", "newpass", "newbio", "newimage");
    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_not_update_fields_when_null_values_passed() {
    User user = new User("old@example.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update(null, null, null, null, null);
    assertThat(user.getEmail(), is("old@example.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("oldbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_not_update_fields_when_empty_values_passed() {
    User user = new User("old@example.com", "oldname", "oldpass", "oldbio", "oldimage");
    user.update("", "", "", "", "");
    assertThat(user.getEmail(), is("old@example.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("oldbio"));
    assertThat(user.getImage(), is("oldimage"));
  }

  @Test
  public void should_equal_when_same_id() {
    User user1 = new User("a@b.com", "u1", "p1", "bio1", "img1");
    User user2 = new User("c@d.com", "u2", "p2", "bio2", "img2");
    assertThat(user1.equals(user1), is(true));
    assertThat(user1.equals(user2), is(false));
  }

  @Test
  public void should_have_same_hashcode_for_same_object() {
    User user = new User("a@b.com", "u1", "p1", "bio1", "img1");
    assertThat(user.hashCode(), is(user.hashCode()));
  }
}
