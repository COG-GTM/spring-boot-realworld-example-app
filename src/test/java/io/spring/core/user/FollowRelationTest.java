package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_all_fields() {
    FollowRelation relation = new FollowRelation("user123", "target456");

    assertThat(relation.getUserId(), is("user123"));
    assertThat(relation.getTargetId(), is("target456"));
  }

  @Test
  public void should_have_equal_relations_with_same_user_and_target() {
    FollowRelation relation1 = new FollowRelation("user123", "target456");
    FollowRelation relation2 = new FollowRelation("user123", "target456");

    assertThat(relation1.equals(relation2), is(true));
    assertThat(relation1.hashCode(), is(relation2.hashCode()));
  }

  @Test
  public void should_have_different_relations_with_different_user() {
    FollowRelation relation1 = new FollowRelation("user123", "target456");
    FollowRelation relation2 = new FollowRelation("user789", "target456");

    assertThat(relation1.equals(relation2), is(false));
  }

  @Test
  public void should_have_different_relations_with_different_target() {
    FollowRelation relation1 = new FollowRelation("user123", "target456");
    FollowRelation relation2 = new FollowRelation("user123", "target789");

    assertThat(relation1.equals(relation2), is(false));
  }

  @Test
  public void should_allow_setting_user_id() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    relation.setUserId("newUser");

    assertThat(relation.getUserId(), is("newUser"));
  }

  @Test
  public void should_allow_setting_target_id() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    relation.setTargetId("newTarget");

    assertThat(relation.getTargetId(), is("newTarget"));
  }

  @Test
  public void should_create_relation_with_empty_ids() {
    FollowRelation relation = new FollowRelation("", "");

    assertThat(relation.getUserId(), is(""));
    assertThat(relation.getTargetId(), is(""));
  }
}
