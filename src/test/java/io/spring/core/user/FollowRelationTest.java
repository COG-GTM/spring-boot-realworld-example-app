package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_user_and_target() {
    FollowRelation followRelation = new FollowRelation("user123", "target456");

    assertThat(followRelation.getUserId(), is("user123"));
    assertThat(followRelation.getTargetId(), is("target456"));
  }

  @Test
  public void should_create_follow_relation_with_no_args_constructor() {
    FollowRelation followRelation = new FollowRelation();

    assertThat(followRelation.getUserId(), nullValue());
    assertThat(followRelation.getTargetId(), nullValue());
  }

  @Test
  public void should_set_user_id() {
    FollowRelation followRelation = new FollowRelation();

    followRelation.setUserId("user123");

    assertThat(followRelation.getUserId(), is("user123"));
  }

  @Test
  public void should_set_target_id() {
    FollowRelation followRelation = new FollowRelation();

    followRelation.setTargetId("target456");

    assertThat(followRelation.getTargetId(), is("target456"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    FollowRelation followRelation1 = new FollowRelation("user123", "target456");
    FollowRelation followRelation2 = new FollowRelation("user123", "target456");

    assertThat(followRelation1.equals(followRelation2), is(true));
    assertThat(followRelation1.hashCode(), is(followRelation2.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    FollowRelation followRelation1 = new FollowRelation("user123", "target456");
    FollowRelation followRelation2 = new FollowRelation("user789", "target456");

    assertThat(followRelation1.equals(followRelation2), is(false));
  }

  @Test
  public void should_not_be_equal_when_target_id_differs() {
    FollowRelation followRelation1 = new FollowRelation("user123", "target456");
    FollowRelation followRelation2 = new FollowRelation("user123", "target789");

    assertThat(followRelation1.equals(followRelation2), is(false));
  }

  @Test
  public void should_have_consistent_hashcode() {
    FollowRelation followRelation = new FollowRelation("user123", "target456");

    int hashCode1 = followRelation.hashCode();
    int hashCode2 = followRelation.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }

  @Test
  public void should_have_different_hashcode_for_different_relations() {
    FollowRelation followRelation1 = new FollowRelation("user123", "target456");
    FollowRelation followRelation2 = new FollowRelation("user789", "target012");

    assertThat(followRelation1.hashCode(), not(followRelation2.hashCode()));
  }

  @Test
  public void should_have_to_string_representation() {
    FollowRelation followRelation = new FollowRelation("user123", "target456");

    String toString = followRelation.toString();

    assertThat(toString.contains("user123"), is(true));
    assertThat(toString.contains("target456"), is(true));
  }
}
