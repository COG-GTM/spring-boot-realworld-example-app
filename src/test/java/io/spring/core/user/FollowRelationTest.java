package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FollowRelationTest {

  @Test
  public void should_be_equal_when_user_and_target_match() {
    FollowRelation a = new FollowRelation("user", "target");
    FollowRelation b = new FollowRelation("user", "target");

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.getUserId()).isEqualTo("user");
    assertThat(a.getTargetId()).isEqualTo("target");
  }

  @Test
  public void should_be_directional() {
    assertThat(new FollowRelation("user", "target"))
        .isNotEqualTo(new FollowRelation("target", "user"));
  }
}
