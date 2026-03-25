package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FollowRelationCoverageTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    assertEquals("user1", relation.getUserId());
    assertEquals("user2", relation.getTargetId());
  }

  @Test
  public void should_have_no_arg_constructor() {
    FollowRelation relation = new FollowRelation();
    assertNull(relation.getUserId());
    assertNull(relation.getTargetId());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    FollowRelation relation1 = new FollowRelation("user1", "user2");
    FollowRelation relation2 = new FollowRelation("user1", "user2");
    FollowRelation relation3 = new FollowRelation("user1", "user3");

    assertEquals(relation1, relation2);
    assertEquals(relation1.hashCode(), relation2.hashCode());
    assertNotEquals(relation1, relation3);
  }

  @Test
  public void should_have_setters() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("user1");
    relation.setTargetId("user2");
    assertEquals("user1", relation.getUserId());
    assertEquals("user2", relation.getTargetId());
  }

  @Test
  public void should_have_toString() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    String str = relation.toString();
    assertNotNull(str);
    assertTrue(str.contains("user1"));
    assertTrue(str.contains("user2"));
  }
}
