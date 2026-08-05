package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_set_fields_from_constructor() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_have_null_fields_with_default_constructor() {
    FollowRelation relation = new FollowRelation();

    assertThat(relation.getUserId(), is(nullValue()));
    assertThat(relation.getTargetId(), is(nullValue()));
  }

  @Test
  public void should_update_fields_via_setters() {
    FollowRelation relation = new FollowRelation();

    relation.setUserId("userId");
    relation.setTargetId("targetId");

    assertThat(relation.getUserId(), is("userId"));
    assertThat(relation.getTargetId(), is("targetId"));
  }

  @Test
  public void should_be_equal_when_all_fields_match() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation other = new FollowRelation("userId", "targetId");

    assertThat(relation.equals(other), is(true));
    assertThat(relation.hashCode(), is(other.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation other = new FollowRelation("otherUserId", "targetId");

    assertThat(relation.equals(other), is(false));
    assertThat(relation.hashCode(), is(not(other.hashCode())));
  }

  @Test
  public void should_not_be_equal_when_target_id_differs() {
    FollowRelation relation = new FollowRelation("userId", "targetId");
    FollowRelation other = new FollowRelation("userId", "otherTargetId");

    assertThat(relation.equals(other), is(false));
    assertThat(relation.hashCode(), is(not(other.hashCode())));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.equals(null), is(false));
    assertThat(relation.equals("userId"), is(false));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    FollowRelation relation = new FollowRelation("userId", "targetId");

    assertThat(relation.toString().contains("userId"), is(true));
    assertThat(relation.toString().contains("targetId"), is(true));
  }
}
