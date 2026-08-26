package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_keep_user_and_target() {
    FollowRelation relation = new FollowRelation("user", "target");
    assertThat(relation.getUserId(), is("user"));
    assertThat(relation.getTargetId(), is("target"));
  }

  @Test
  public void should_be_equal_with_same_user_and_target() {
    assertThat(new FollowRelation("user", "target"), is(new FollowRelation("user", "target")));
    assertThat(
        new FollowRelation("user", "target").hashCode(),
        is(new FollowRelation("user", "target").hashCode()));
  }

  @Test
  public void should_not_be_equal_when_target_differs() {
    assertThat(new FollowRelation("user", "target"), not(new FollowRelation("user", "other")));
  }
}
