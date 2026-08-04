package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

public class UserTest {

  private User newUser() {
    return new User("old@test.com", "oldname", "oldpass", "old bio", "old-image.png");
  }

  @Test
  public void should_generate_id_and_assign_fields_on_construction() {
    User user = newUser();

    assertNotNull(user.getId());
    assertThat(user.getEmail(), is("old@test.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("old bio"));
    assertThat(user.getImage(), is("old-image.png"));
  }

  @Test
  public void should_update_only_email() {
    User user = newUser();

    user.update("new@test.com", "", "", "", "");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("old bio"));
    assertThat(user.getImage(), is("old-image.png"));
  }

  @Test
  public void should_update_only_username() {
    User user = newUser();

    user.update("", "newname", "", "", "");

    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getEmail(), is("old@test.com"));
  }

  @Test
  public void should_update_only_password() {
    User user = newUser();

    user.update("", "", "newpass", "", "");

    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getEmail(), is("old@test.com"));
  }

  @Test
  public void should_update_only_bio() {
    User user = newUser();

    user.update("", "", "", "new bio", "");

    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getEmail(), is("old@test.com"));
  }

  @Test
  public void should_update_only_image() {
    User user = newUser();

    user.update("", "", "", "", "new-image.png");

    assertThat(user.getImage(), is("new-image.png"));
    assertThat(user.getEmail(), is("old@test.com"));
  }

  @Test
  public void should_update_all_fields_when_all_non_blank() {
    User user = newUser();

    user.update("new@test.com", "newname", "newpass", "new bio", "new-image.png");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new-image.png"));
  }

  @Test
  public void should_leave_all_fields_untouched_for_empty_or_null_arguments() {
    User user = newUser();

    user.update("", "", "", "", "");
    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("old@test.com"));
    assertThat(user.getUsername(), is("oldname"));
    assertThat(user.getPassword(), is("oldpass"));
    assertThat(user.getBio(), is("old bio"));
    assertThat(user.getImage(), is("old-image.png"));
  }

  @Test
  public void should_use_only_id_for_equality() {
    User a = newUser();
    User b = new User("other@test.com", "other", "otherpass", "other bio", "other-image.png");

    assertThat(a, is(a));
    assertThat(a, is(not(b)));
    assertThat(a.hashCode(), is(a.hashCode()));
  }

  @Test
  public void should_be_equal_when_only_id_matches_despite_other_fields() throws Exception {
    User a = new User("a@test.com", "a", "pa", "bio-a", "image-a.png");
    User b = new User("b@test.com", "b", "pb", "bio-b", "image-b.png");
    setId(b, a.getId());

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
  }

  private static void setId(User user, String id) throws Exception {
    Field field = User.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(user, id);
  }
}
