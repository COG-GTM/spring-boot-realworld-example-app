package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_set_all_fields_and_generate_id_on_construction() {
    User user = new User("jake@jake.jake", "jake", "password", "bio", "image");
    assertThat(user.getId(), is(notNullValue()));
    assertThat(user.getEmail(), is("jake@jake.jake"));
    assertThat(user.getUsername(), is("jake"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_generate_unique_ids_for_different_users() {
    User first = new User("a@a.com", "a", "p", "b", "i");
    User second = new User("b@b.com", "b", "p", "b", "i");
    assertThat(first.getId(), is(not(second.getId())));
  }

  @Test
  public void should_update_all_fields_when_new_values_provided() {
    User user = new User("jake@jake.jake", "jake", "password", "bio", "image");
    user.update("new@email.com", "newname", "newpassword", "new bio", "new image");
    assertThat(user.getEmail(), is("new@email.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_keep_existing_values_when_update_args_are_empty() {
    User user = new User("jake@jake.jake", "jake", "password", "bio", "image");
    user.update("", "", "", "", "");
    assertThat(user.getEmail(), is("jake@jake.jake"));
    assertThat(user.getUsername(), is("jake"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_only_the_provided_fields() {
    User user = new User("jake@jake.jake", "jake", "password", "bio", "image");
    user.update("", "newname", "", "", "new image");
    assertThat(user.getEmail(), is("jake@jake.jake"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_be_equal_for_distinct_instances_sharing_the_same_id() throws Exception {
    User first = new User("jake@jake.jake", "jake", "password", "bio", "image");
    User second = new User("other@email.com", "other", "otherpassword", "other bio", "other image");
    setId(second, first.getId());
    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_ids_differ() {
    User first = new User("a@a.com", "a", "p", "b", "i");
    User second = new User("b@b.com", "b", "p", "b", "i");
    assertThat(first, is(not(second)));
  }

  private static void setId(User user, String id) throws Exception {
    Field field = User.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(user, id);
  }
}
