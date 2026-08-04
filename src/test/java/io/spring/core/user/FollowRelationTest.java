package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_assign_user_and_target_ids() {
    FollowRelation relation = new FollowRelation("user-1", "target-1");

    assertThat(relation.getUserId(), is("user-1"));
    assertThat(relation.getTargetId(), is("target-1"));
  }

  @Test
  public void should_compare_by_all_fields() {
    FollowRelation a = new FollowRelation("user-1", "target-1");

    assertThat(a, is(new FollowRelation("user-1", "target-1")));
    assertThat(a.hashCode(), is(new FollowRelation("user-1", "target-1").hashCode()));
    assertThat(a, is(not(new FollowRelation("user-1", "target-2"))));
    assertThat(a, is(not(new FollowRelation("user-2", "target-1"))));
  }
}
