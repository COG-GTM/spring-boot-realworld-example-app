package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import graphql.ExecutionResult;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProfileDatafetcherTest extends GraphQLTestBase {
  private User target;

  @BeforeEach
  public void setUp() {
    target = new User("target@test.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
  }

  @Test
  public void should_query_profile() {
    when(profileQueryService.findByUsername(eq(target.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), target.getUsername(), "bio", "image", false)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \"target\") { profile { username bio image following } } }");

    assertThat(context.<String>read("data.profile.profile.username")).isEqualTo("target");
    assertThat(context.<String>read("data.profile.profile.bio")).isEqualTo("bio");
    assertThat(context.<String>read("data.profile.profile.image")).isEqualTo("image");
    assertThat(context.<Boolean>read("data.profile.profile.following")).isFalse();
  }

  @Test
  public void should_return_error_for_unknown_profile() {
    when(profileQueryService.findByUsername(eq("unknown"), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"unknown\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
  }

  @Test
  public void should_follow_user() {
    loginAs(user);
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), target.getUsername(), "bio", "image", true)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { followUser(username: \"target\") { profile { username following } } }");

    assertThat(context.<String>read("data.followUser.profile.username")).isEqualTo("target");
    assertThat(context.<Boolean>read("data.followUser.profile.following")).isTrue();
    verify(userRepository).saveRelation(eq(new FollowRelation(user.getId(), target.getId())));
  }

  @Test
  public void should_reject_follow_without_authentication() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"target\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_user() {
    loginAs(user);
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(
            Optional.of(
                new ProfileData(target.getId(), target.getUsername(), "bio", "image", false)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { unfollowUser(username: \"target\") { profile { username following } }"
                + " }");

    assertThat(context.<Boolean>read("data.unfollowUser.profile.following")).isFalse();
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_return_error_when_unfollowing_user_not_followed() {
    loginAs(user);
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"target\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(userRepository, never()).removeRelation(any());
  }
}
