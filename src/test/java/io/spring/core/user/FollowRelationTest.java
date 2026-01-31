package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_user_and_target_id() {
    String userId = "user123";
    String targetId = "user456";

    FollowRelation relation = new FollowRelation(userId, targetId);

    assertThat(relation.getUserId(), is(userId));
    assertThat(relation.getTargetId(), is(targetId));
  }

  @Test
  public void should_have_equality_based_on_both_ids() {
    FollowRelation relation1 = new FollowRelation("user1", "user2");
    FollowRelation relation2 = new FollowRelation("user1", "user2");
    FollowRelation relation3 = new FollowRelation("user1", "user3");
    FollowRelation relation4 = new FollowRelation("user2", "user2");

    assertThat(relation1, is(relation2));
    assertThat(relation1.hashCode(), is(relation2.hashCode()));
    assertThat(relation1, not(relation3));
    assertThat(relation1, not(relation4));
  }

  @Test
  public void should_allow_setting_user_id() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    relation.setUserId("user3");

    assertThat(relation.getUserId(), is("user3"));
  }

  @Test
  public void should_allow_setting_target_id() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    relation.setTargetId("user4");

    assertThat(relation.getTargetId(), is("user4"));
  }
}
