package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation_with_user_id() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    assertThat(relation.getUserId(), is("user123"));
  }

  @Test
  public void should_create_follow_relation_with_target_id() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    assertThat(relation.getTargetId(), is("target456"));
  }
}
