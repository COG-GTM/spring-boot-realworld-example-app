package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FollowRelationTest {

  @Test
  void should_hold_follower_and_target_ids() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  void should_allow_mutating_fields() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("userId");
    relation.setTargetId("targetId");

    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  void should_be_directional() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation reversed = new FollowRelation("targetId", "userId");

    assertThat(relation).isNotEqualTo(reversed);
  }

  @Test
  void should_compare_by_both_ids() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation same = new FollowRelation("userId", "targetId");

    assertThat(relation).isEqualTo(relation).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(relation).isNotEqualTo(new FollowRelation("other", "targetId"));
    assertThat(relation).isNotEqualTo(new FollowRelation("userId", "other"));
    assertThat(relation).isNotEqualTo(new FollowRelation());
    assertThat(relation).isNotEqualTo(null).isNotEqualTo("relation");
    assertThat(relation.toString()).contains("userId").contains("targetId");
  }
}
