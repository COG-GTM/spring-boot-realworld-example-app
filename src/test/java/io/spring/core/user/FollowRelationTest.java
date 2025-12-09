package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_ids() {
    FollowRelation relation = new FollowRelation("user-123", "target-456");

    assertEquals("user-123", relation.getUserId());
    assertEquals("target-456", relation.getTargetId());
  }

  @Test
  public void should_have_equals_based_on_all_fields() {
    FollowRelation relation1 = new FollowRelation("user-123", "target-456");
    FollowRelation relation2 = new FollowRelation("user-123", "target-456");
    FollowRelation relation3 = new FollowRelation("user-123", "target-789");
    FollowRelation relation4 = new FollowRelation("user-999", "target-456");

    assertEquals(relation1, relation2);
    assertNotEquals(relation1, relation3);
    assertNotEquals(relation1, relation4);
  }

  @Test
  public void should_have_same_hashcode_for_equal_objects() {
    FollowRelation relation1 = new FollowRelation("user-123", "target-456");
    FollowRelation relation2 = new FollowRelation("user-123", "target-456");

    assertEquals(relation1.hashCode(), relation2.hashCode());
  }

  @Test
  public void should_allow_setting_user_id() {
    FollowRelation relation = new FollowRelation("user-123", "target-456");
    relation.setUserId("user-999");

    assertEquals("user-999", relation.getUserId());
  }

  @Test
  public void should_allow_setting_target_id() {
    FollowRelation relation = new FollowRelation("user-123", "target-456");
    relation.setTargetId("target-999");

    assertEquals("target-999", relation.getTargetId());
  }
}
