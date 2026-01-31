package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_follow_relation() {
    FollowRelation relation = new FollowRelation("user123", "target456");
    
    assertThat(relation.getUserId(), is("user123"));
    assertThat(relation.getTargetId(), is("target456"));
  }

  @Test
  public void should_have_equals_and_hashcode() {
    FollowRelation relation1 = new FollowRelation("user123", "target456");
    FollowRelation relation2 = new FollowRelation("user123", "target456");
    
    assertThat(relation1.equals(relation2), is(true));
    assertThat(relation1.hashCode(), is(relation2.hashCode()));
  }
}
