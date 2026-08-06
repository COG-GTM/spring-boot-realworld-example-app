package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_assign_user_id_and_target_id() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation.getUserId()).isEqualTo("user-id");
    assertThat(relation.getTargetId()).isEqualTo("target-id");
  }

  @Test
  public void should_be_equal_when_both_ids_match() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    FollowRelation same = new FollowRelation("user-id", "target-id");

    assertThat(relation).isEqualTo(same);
    assertThat(relation.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_not_be_equal_when_direction_is_reversed() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    FollowRelation reversed = new FollowRelation("target-id", "user-id");

    assertThat(relation).isNotEqualTo(reversed);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation).isNotEqualTo(null);
    assertThat(relation).isNotEqualTo("not a relation");
  }

  @Test
  public void should_support_no_args_constructor_and_setters() {
    FollowRelation relation = new FollowRelation();

    assertThat(relation.getUserId()).isNull();
    assertThat(relation.getTargetId()).isNull();

    relation.setUserId("user-id");
    relation.setTargetId("target-id");

    assertThat(relation.getUserId()).isEqualTo("user-id");
    assertThat(relation.getTargetId()).isEqualTo("target-id");
    assertThat(relation.toString()).contains("user-id", "target-id");
  }
}
