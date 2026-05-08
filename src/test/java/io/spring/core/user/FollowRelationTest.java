package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_store_user_id_and_target_id() {
    FollowRelation relation = new FollowRelation("user-1", "target-1");

    assertEquals("user-1", relation.getUserId());
    assertEquals("target-1", relation.getTargetId());
  }

  @Test
  public void no_args_constructor_should_leave_fields_null() {
    FollowRelation relation = new FollowRelation();
    assertEquals(null, relation.getUserId());
    assertEquals(null, relation.getTargetId());
  }
}
