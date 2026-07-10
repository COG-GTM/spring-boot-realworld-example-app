package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_set_user_id_and_target_id_on_construction() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_update_fields_via_setters() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("userId");
    relation.setTargetId("targetId");
    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_be_equal_for_same_user_and_target() {
    FollowRelation first = new FollowRelation("userId", "targetId");
    FollowRelation second = new FollowRelation("userId", "targetId");
    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_fields_differ() {
    FollowRelation first = new FollowRelation("userId", "targetId");
    FollowRelation second = new FollowRelation("userId", "otherTarget");
    assertThat(first, is(not(second)));
  }
}
