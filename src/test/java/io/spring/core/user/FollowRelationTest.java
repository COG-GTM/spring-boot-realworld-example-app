package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_correct_fields() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    assertThat(relation.getUserId(), is("user-id"));
    assertThat(relation.getTargetId(), is("target-id"));
  }

  @Test
  public void should_be_equal_when_same_user_and_target() {
    FollowRelation r1 = new FollowRelation("user-id", "target-id");
    FollowRelation r2 = new FollowRelation("user-id", "target-id");
    assertThat(r1, is(r2));
    assertThat(r1.hashCode(), is(r2.hashCode()));
  }

  @Test
  public void should_support_setters() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("uid");
    relation.setTargetId("tid");
    assertThat(relation.getUserId(), is("uid"));
    assertThat(relation.getTargetId(), is("tid"));
  }
}
