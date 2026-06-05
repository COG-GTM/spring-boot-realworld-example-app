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
    FollowRelation r1 = new FollowRelation("user-1", "user-2");
    FollowRelation r2 = new FollowRelation("user-1", "user-2");
    assertThat(r1.equals(r2), is(true));
  }

  @Test
  public void should_not_be_equal_with_different_user_id() {
    FollowRelation r1 = new FollowRelation("user-1", "user-2");
    FollowRelation r2 = new FollowRelation("user-3", "user-2");
    assertThat(r1.equals(r2), is(false));
  }

  @Test
  public void should_not_be_equal_with_different_target_id() {
    FollowRelation r1 = new FollowRelation("user-1", "user-2");
    FollowRelation r2 = new FollowRelation("user-1", "user-3");
    assertThat(r1.equals(r2), is(false));
  }

  @Test
  public void should_not_be_equal_when_user_follows_self_vs_other() {
    FollowRelation selfFollow = new FollowRelation("user-1", "user-1");
    FollowRelation otherFollow = new FollowRelation("user-1", "user-2");
    assertThat(selfFollow.equals(otherFollow), is(false));
  }
}
