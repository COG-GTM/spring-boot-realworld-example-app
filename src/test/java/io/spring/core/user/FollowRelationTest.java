package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_assign_all_fields_from_constructor() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_have_null_fields_when_created_with_default_constructor() {
    FollowRelation relation = new FollowRelation();

    assertThat(relation.getUserId(), is(nullValue()));
    assertThat(relation.getTargetId(), is(nullValue()));
  }

  @Test
  public void should_be_equal_when_both_ids_are_equal() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation same = new FollowRelation("userId", "targetId");

    assertThat(relation, is(same));
    assertThat(relation.hashCode(), is(same.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_any_id_differs() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation, is(not(new FollowRelation("otherUserId", "targetId"))));
    assertThat(relation, is(not(new FollowRelation("userId", "otherTargetId"))));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.equals(null), is(false));
    assertThat(relation.equals("userId"), is(false));
  }

  @Test
  public void should_reflect_setter_changes() {
    FollowRelation relation = new FollowRelation();

    relation.setUserId("userId");
    relation.setTargetId("targetId");

    assertThat(relation, is(new FollowRelation("userId", "targetId")));
  }
}
