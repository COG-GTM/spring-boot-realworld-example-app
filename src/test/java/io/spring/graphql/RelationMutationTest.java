package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RelationMutationTest extends GraphQLTestBase {

  private User target;

  @BeforeEach
  public void setUpFixtures() {
    target = new User("celeb@test.com", "celeb", "123", "bio", defaultAvatar);
    when(userRepository.findByUsername(eq("celeb"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("celeb"), any()))
        .thenReturn(Optional.of(new ProfileData(target.getId(), "celeb", "bio", defaultAvatar, true)));
  }

  @Test
  public void should_follow_user() {
    String followedUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { followUser(username: \"celeb\") { profile { username following } } }",
            "data.followUser.profile.username");

    Assertions.assertEquals("celeb", followedUsername);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_reject_follow_when_anonymous() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"celeb\") { profile { username } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_return_error_when_follow_target_missing() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_unfollow_user() {
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { unfollowUser(username: \"celeb\") { profile { username } } }",
            "data.unfollowUser.profile.username");

    Assertions.assertEquals("celeb", username);
    verify(userRepository).removeRelation(relation);
  }

  @Test
  public void should_return_error_when_unfollow_without_relation() {
    when(userRepository.findRelation(any(), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"celeb\") { profile { username } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }
}
