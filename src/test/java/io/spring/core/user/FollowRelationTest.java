package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_keep_user_and_target_id() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation.getUserId()).isEqualTo("user-id");
    assertThat(relation.getTargetId()).isEqualTo("target-id");
  }

  @Test
  public void should_use_all_fields_for_equality() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation).isEqualTo(new FollowRelation("user-id", "target-id"));
    assertThat(relation.hashCode())
        .isEqualTo(new FollowRelation("user-id", "target-id").hashCode());
    assertThat(relation).isNotEqualTo(new FollowRelation("target-id", "user-id"));
    assertThat(relation.toString()).contains("user-id").contains("target-id");
  }

  @Test
  public void should_support_no_args_constructor_and_setters() {
    FollowRelation relation = new FollowRelation();
    assertThat(relation.getUserId()).isNull();
    assertThat(relation.getTargetId()).isNull();

    relation.setUserId("user-id");
    relation.setTargetId("target-id");

    assertThat(relation).isEqualTo(new FollowRelation("user-id", "target-id"));
  }
}
