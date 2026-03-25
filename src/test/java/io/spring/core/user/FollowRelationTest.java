package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user1", "target1");
    assertEquals("user1", relation.getUserId());
    assertEquals("target1", relation.getTargetId());
  }

  @Test
  public void should_equal_with_same_fields() {
    FollowRelation r1 = new FollowRelation("user1", "target1");
    FollowRelation r2 = new FollowRelation("user1", "target1");
    assertEquals(r1, r2);
  }

  @Test
  public void should_not_equal_with_different_fields() {
    FollowRelation r1 = new FollowRelation("user1", "target1");
    FollowRelation r2 = new FollowRelation("user2", "target1");
    assertNotEquals(r1, r2);
  }

  @Test
  public void should_have_same_hashcode_for_equal() {
    FollowRelation r1 = new FollowRelation("user1", "target1");
    FollowRelation r2 = new FollowRelation("user1", "target1");
    assertEquals(r1.hashCode(), r2.hashCode());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    FollowRelation relation = new FollowRelation();
    assertNull(relation.getUserId());
    assertNull(relation.getTargetId());
  }

  @Test
  public void should_set_fields() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("user1");
    relation.setTargetId("target1");
    assertEquals("user1", relation.getUserId());
    assertEquals("target1", relation.getTargetId());
  }

  @Test
  public void should_have_toString() {
    FollowRelation relation = new FollowRelation("user1", "target1");
    assertNotNull(relation.toString());
    assertTrue(relation.toString().contains("user1"));
    assertTrue(relation.toString().contains("target1"));
  }
}
