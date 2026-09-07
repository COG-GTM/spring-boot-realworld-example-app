package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_construct_relation_and_get_values() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");

    assertThat(relation.getUserId(), is("user-id"));
    assertThat(relation.getTargetId(), is("target-id"));
    assertThat(relation.equals(new FollowRelation("user-id", "target-id")), is(true));
  }
}
