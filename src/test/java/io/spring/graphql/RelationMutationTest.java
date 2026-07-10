package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RelationMutationTest extends DgsGraphQLTestBase {

  private final User target = new User("target@test.com", "target", "123", "bio", "img");

  @Test
  void should_follow_user() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq(target.getUsername()), any()))
        .thenReturn(Optional.of(profileData(target)));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { followUser(username: \"target\") { profile { username following } } }",
            "data.followUser.profile.username");

    assertEquals(target.getUsername(), username);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void should_reject_follow_when_not_authenticated() {
    setAnonymous();

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { followUser(username: \"target\") { profile { username } } }",
                    "data.followUser"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_follow_when_target_missing() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { followUser(username: \"ghost\") { profile { username } } }",
                    "data.followUser"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_unfollow_user() {
    setAuthenticatedUser(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(target.getUsername()), any()))
        .thenReturn(Optional.of(profileData(target)));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { unfollowUser(username: \"target\") { profile { username } } }",
            "data.unfollowUser.profile.username");

    assertEquals(target.getUsername(), username);
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  void should_reject_unfollow_when_target_missing() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { unfollowUser(username: \"ghost\") { profile { username } } }",
                    "data.unfollowUser"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_unfollow_when_relation_missing() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(any(), any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { unfollowUser(username: \"target\") { profile { username } } }",
                    "data.unfollowUser"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_unfollow_when_not_authenticated() {
    setAnonymous();

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { unfollowUser(username: \"target\") { profile { username } } }",
                    "data.unfollowUser"));

    assertFalse(error.getErrors().isEmpty());
  }
}
