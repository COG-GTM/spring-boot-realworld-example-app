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
  public void should_have_equal_relations_with_same_fields() {
    FollowRelation relation1 = new FollowRelation("user1", "user2");
    FollowRelation relation2 = new FollowRelation("user1", "user2");

    assertEquals(relation1, relation2);
    assertEquals(relation1.hashCode(), relation2.hashCode());
  }

  @Test
  public void should_have_unequal_relations_with_different_fields() {
    FollowRelation relation1 = new FollowRelation("user1", "user2");
    FollowRelation relation2 = new FollowRelation("user1", "user3");

    assertNotEquals(relation1, relation2);
  }

  @Test
  public void should_have_unequal_relations_with_different_user_ids() {
    FollowRelation relation1 = new FollowRelation("user1", "user2");
    FollowRelation relation2 = new FollowRelation("user3", "user2");

    assertNotEquals(relation1, relation2);
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    FollowRelation relation = new FollowRelation();
    assertNull(relation.getUserId());
    assertNull(relation.getTargetId());
  }

  @Test
  public void should_not_equal_null() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    assertNotEquals(null, relation);
  }

  @Test
  public void should_not_equal_different_type() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    assertNotEquals("not a relation", relation);
  }

  @Test
  public void should_equal_itself() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    assertEquals(relation, relation);
  }

  @Test
  public void should_have_toString() {
    FollowRelation relation = new FollowRelation("user1", "user2");
    String str = relation.toString();
    assertNotNull(str);
    assertTrue(str.contains("user1"));
    assertTrue(str.contains("user2"));
  }

  @Test
  public void should_have_unequal_with_null_user_id() {
    FollowRelation relation1 = new FollowRelation(null, "user2");
    FollowRelation relation2 = new FollowRelation("user1", "user2");
    assertNotEquals(relation1, relation2);
  }

  @Test
  public void should_have_unequal_with_null_target_id() {
    FollowRelation relation1 = new FollowRelation("user1", null);
    FollowRelation relation2 = new FollowRelation("user1", "user2");
    assertNotEquals(relation1, relation2);
  }
}
