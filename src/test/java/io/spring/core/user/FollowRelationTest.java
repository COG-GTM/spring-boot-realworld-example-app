package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_set_user_and_target_ids_on_construction() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  public void should_allow_setting_ids_via_setters() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("userId");
    relation.setTargetId("targetId");
    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  public void should_be_equal_when_both_ids_match() {
    FollowRelation first = new FollowRelation("userId", "targetId");
    FollowRelation second = new FollowRelation("userId", "targetId");
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    FollowRelation first = new FollowRelation("userId", "targetId");
    FollowRelation second = new FollowRelation("otherUser", "targetId");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_not_be_equal_when_target_id_differs() {
    FollowRelation first = new FollowRelation("userId", "targetId");
    FollowRelation second = new FollowRelation("userId", "otherTarget");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void should_allow_null_ids() {
    FollowRelation relation = new FollowRelation(null, null);
    assertThat(relation.getUserId()).isNull();
    assertThat(relation.getTargetId()).isNull();
  }
}
