package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user-1", "user-2");

    assertThat(relation.getUserId(), is("user-1"));
    assertThat(relation.getTargetId(), is("user-2"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    FollowRelation relation1 = new FollowRelation("user-1", "user-2");
    FollowRelation relation2 = new FollowRelation("user-1", "user-2");

    assertThat(relation1.equals(relation2), is(true));
    assertThat(relation1.hashCode(), is(relation2.hashCode()));
  }

  @Test
  public void should_not_be_equal_with_different_user_ids() {
    FollowRelation relation1 = new FollowRelation("user-1", "user-2");
    FollowRelation relation2 = new FollowRelation("user-3", "user-2");

    assertThat(relation1.equals(relation2), is(false));
  }

  @Test
  public void should_not_be_equal_with_different_target_ids() {
    FollowRelation relation1 = new FollowRelation("user-1", "user-2");
    FollowRelation relation2 = new FollowRelation("user-1", "user-3");

    assertThat(relation1.equals(relation2), is(false));
  }
}
