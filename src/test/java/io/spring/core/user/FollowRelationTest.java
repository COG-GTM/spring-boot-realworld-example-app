package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("userId1", "targetId1");
    assertEquals("userId1", relation.getUserId());
    assertEquals("targetId1", relation.getTargetId());
  }

  @Test
  void should_support_no_arg_constructor() {
    FollowRelation relation = new FollowRelation();
    assertNull(relation.getUserId());
    assertNull(relation.getTargetId());
  }

  @Test
  void should_support_setters() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("userId1");
    relation.setTargetId("targetId1");
    assertEquals("userId1", relation.getUserId());
    assertEquals("targetId1", relation.getTargetId());
  }

  @Test
  void should_have_equality_based_on_all_fields() {
    FollowRelation relation1 = new FollowRelation("userId1", "targetId1");
    FollowRelation relation2 = new FollowRelation("userId1", "targetId1");
    assertEquals(relation1, relation2);
  }

  @Test
  void should_not_equal_different_follow_relation() {
    FollowRelation relation1 = new FollowRelation("userId1", "targetId1");
    FollowRelation relation2 = new FollowRelation("userId1", "targetId2");
    assertNotEquals(relation1, relation2);
  }
}
