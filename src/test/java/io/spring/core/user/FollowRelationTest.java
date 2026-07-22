package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_keep_constructor_values() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_support_setters_and_no_args_constructor() {
    FollowRelation relation = new FollowRelation();
    relation.setUserId("userId");
    relation.setTargetId("targetId");
    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_be_equal_when_fields_are_equal() {
    FollowRelation one = new FollowRelation("userId", "targetId");
    FollowRelation two = new FollowRelation("userId", "targetId");
    FollowRelation different = new FollowRelation("userId", "other");
    assertThat(one.equals(two), is(true));
    assertThat(one.hashCode(), is(two.hashCode()));
    assertThat(one.equals(different), is(false));
  }
}
