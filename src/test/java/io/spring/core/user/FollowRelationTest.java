package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    assertEquals("user1", relation.getUserId());
    assertEquals("user2", relation.getTargetId());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    FollowRelation r1 = new FollowRelation("user1", "user2");
    FollowRelation r2 = new FollowRelation("user1", "user2");
    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
  }

  @Test
  public void should_not_be_equal_for_different_relations() {
    FollowRelation r1 = new FollowRelation("user1", "user2");
    FollowRelation r2 = new FollowRelation("user1", "user3");
    assertNotEquals(r1, r2);
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    FollowRelation relation = new FollowRelation();
    assertNull(relation.getUserId());
    assertNull(relation.getTargetId());
  }
}
