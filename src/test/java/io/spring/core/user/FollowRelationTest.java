package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_keep_both_ids_from_constructor() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  public void should_leave_both_ids_null_with_no_args_constructor() {
    FollowRelation relation = new FollowRelation();

    assertThat(relation.getUserId()).isNull();
    assertThat(relation.getTargetId()).isNull();
  }

  @Test
  public void should_set_ids_with_setters() {
    FollowRelation relation = new FollowRelation();

    relation.setUserId("userId");
    relation.setTargetId("targetId");

    assertThat(relation.getUserId()).isEqualTo("userId");
    assertThat(relation.getTargetId()).isEqualTo("targetId");
  }

  @Test
  public void should_use_all_fields_for_equals_and_hashcode() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation same = new FollowRelation("userId", "targetId");

    assertThat(same).isEqualTo(relation);
    assertThat(same.hashCode()).isEqualTo(relation.hashCode());
  }

  @Test
  public void should_not_be_equal_when_any_field_differs() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(new FollowRelation("otherUserId", "targetId")).isNotEqualTo(relation);
    assertThat(new FollowRelation("userId", "otherTargetId")).isNotEqualTo(relation);
    assertThat(new FollowRelation()).isNotEqualTo(relation);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation).isNotEqualTo(null);
    assertThat(relation).isNotEqualTo("userId");
    assertThat(relation).isEqualTo(relation);
  }

  @Test
  public void should_expose_both_ids_in_to_string() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.toString()).contains("userId").contains("targetId");
  }
}
