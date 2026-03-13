package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_set_fields_via_constructor() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_equal_when_same_fields() {
    FollowRelation rel1 = new FollowRelation("userId", "targetId");
    FollowRelation rel2 = new FollowRelation("userId", "targetId");
    assertThat(rel1.equals(rel2), is(true));
    assertThat(rel1.hashCode(), is(rel2.hashCode()));
  }

  @Test
  public void should_not_equal_when_different_fields() {
    FollowRelation rel1 = new FollowRelation("user1", "target1");
    FollowRelation rel2 = new FollowRelation("user2", "target2");
    assertThat(rel1.equals(rel2), is(false));
  }
}
