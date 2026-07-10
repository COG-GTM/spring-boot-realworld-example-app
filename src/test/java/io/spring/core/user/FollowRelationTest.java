package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_set_all_fields_in_constructor() {
    FollowRelation relation = new FollowRelation("user-1", "target-1");

    assertEquals("user-1", relation.getUserId());
    assertEquals("target-1", relation.getTargetId());
  }

  @Test
  public void should_expose_setters_from_data_annotation() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("user-2");
    relation.setTargetId("target-2");

    assertEquals("user-2", relation.getUserId());
    assertEquals("target-2", relation.getTargetId());
  }

  @Test
  public void should_be_equal_and_share_hashcode_when_all_fields_are_equal() {
    FollowRelation first = new FollowRelation("user-1", "target-1");
    FollowRelation second = new FollowRelation("user-1", "target-1");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_any_field_differs() {
    FollowRelation base = new FollowRelation("user-1", "target-1");

    assertNotEquals(base, new FollowRelation("user-2", "target-1"));
    assertNotEquals(base, new FollowRelation("user-1", "target-2"));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    FollowRelation relation = new FollowRelation("user-1", "target-1");

    assertNotEquals(relation, null);
    assertNotEquals(relation, "a string");
    assertTrue(relation.equals(relation));
  }
}
