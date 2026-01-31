package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation.getUserId(), is("user-id"));
    assertThat(relation.getTargetId(), is("target-id"));
  }

  @Test
  public void should_create_empty_follow_relation() {
    FollowRelation relation = new FollowRelation();

    assertNotNull(relation);
  }

  @Test
  public void should_set_follow_relation_fields() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("new-user-id");
    relation.setTargetId("new-target-id");

    assertThat(relation.getUserId(), is("new-user-id"));
    assertThat(relation.getTargetId(), is("new-target-id"));
  }

  @Test
  public void should_implement_equals_and_hashcode() {
    FollowRelation relation1 = new FollowRelation("user-id", "target-id");
    FollowRelation relation2 = new FollowRelation("user-id", "target-id");
    FollowRelation relation3 = new FollowRelation("other-user", "other-target");

    assertEquals(relation1, relation2);
    assertNotEquals(relation1, relation3);
    assertEquals(relation1.hashCode(), relation2.hashCode());
    assertThat(relation1.hashCode(), not(relation3.hashCode()));
  }

  @Test
  public void should_implement_toString() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    String toString = relation.toString();
    assertThat(toString.contains("user-id"), is(true));
    assertThat(toString.contains("target-id"), is(true));
  }
}
