package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_user_and_target() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    
    assertThat(relation.getUserId(), is("user123"));
    assertThat(relation.getTargetId(), is("target456"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    FollowRelation relation1 = new FollowRelation("user1", "target1");
    FollowRelation relation2 = new FollowRelation("user1", "target1");
    FollowRelation relation3 = new FollowRelation("user1", "target2");
    
    assertThat(relation1.equals(relation2), is(true));
    assertThat(relation1.equals(relation3), is(false));
  }

  @Test
  public void should_have_same_hashcode_for_equal_relations() {
    FollowRelation relation1 = new FollowRelation("user1", "target1");
    FollowRelation relation2 = new FollowRelation("user1", "target1");
    
    assertThat(relation1.hashCode(), is(relation2.hashCode()));
  }

  @Test
  public void should_allow_setting_user_id() {
    FollowRelation relation = new FollowRelation("user1", "target1");
    relation.setUserId("user2");
    
    assertThat(relation.getUserId(), is("user2"));
  }

  @Test
  public void should_allow_setting_target_id() {
    FollowRelation relation = new FollowRelation("user1", "target1");
    relation.setTargetId("target2");
    
    assertThat(relation.getTargetId(), is("target2"));
  }

  @Test
  public void should_create_empty_follow_relation_with_no_args_constructor() {
    FollowRelation relation = new FollowRelation();
    
    assertThat(relation.getUserId() == null, is(true));
    assertThat(relation.getTargetId() == null, is(true));
  }
}
