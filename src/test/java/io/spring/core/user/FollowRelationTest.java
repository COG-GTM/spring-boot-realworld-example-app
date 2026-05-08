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
  public void should_have_equality_based_on_all_fields() {
    FollowRelation r1 = new FollowRelation("user1", "user2");
    FollowRelation r2 = new FollowRelation("user1", "user2");
    assertEquals(r1, r2);
  }

  @Test
  public void should_not_be_equal_with_different_user() {
    FollowRelation r1 = new FollowRelation("user1", "user2");
    FollowRelation r2 = new FollowRelation("user3", "user2");
    assertNotEquals(r1, r2);
  }

  @Test
  public void should_not_be_equal_with_different_target() {
    FollowRelation r1 = new FollowRelation("user1", "user2");
    FollowRelation r2 = new FollowRelation("user1", "user3");
    assertNotEquals(r1, r2);
  }
}
