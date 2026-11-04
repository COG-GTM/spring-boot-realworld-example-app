package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_create_relation_between_users() {
    User user = new User("a@test.com", "a", "123", "", "");
    User target = new User("b@test.com", "b", "123", "", "");

    FollowRelation relation = new FollowRelation(user.getId(), target.getId());

    assertThat(relation.getUserId(), is(user.getId()));
    assertThat(relation.getTargetId(), is(target.getId()));
  }

  @Test
  public void should_compare_relations_by_value() {
    FollowRelation relation = new FollowRelation("user", "target");

    assertThat(relation, is(new FollowRelation("user", "target")));
    assertThat(relation.hashCode(), is(new FollowRelation("user", "target").hashCode()));
    assertThat(relation, not(new FollowRelation("target", "user")));
  }

  @Test
  public void should_support_no_args_construction_and_setters() {
    FollowRelation relation = new FollowRelation();
    assertThat(relation.getUserId(), nullValue());
    assertThat(relation.getTargetId(), nullValue());

    relation.setUserId("user");
    relation.setTargetId("target");

    assertThat(relation, is(new FollowRelation("user", "target")));
  }
}
