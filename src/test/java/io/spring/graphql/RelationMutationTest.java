package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
class RelationMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private ProfileQueryService profileQueryService;

  private User current;
  private User target;

  @BeforeEach
  void setUp() {
    current = userFixture("john");
    target = userFixture("jane");
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
  }

  @Test
  void should_follow_user() {
    authenticate(current);
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(current)))
        .thenReturn(Optional.of(profileDataOf(target, true)));

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { followUser(username: \"%s\") { profile { username following } } }",
                target.getUsername()),
            "data.followUser.profile.following");

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(current.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
    assertThat(following).isTrue();
  }

  @Test
  void should_not_follow_user_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { followUser(username: \"%s\") { profile { username } } }",
                target.getUsername()));

    assertFailedWith(result, AuthenticationException.class);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_return_error_when_following_unknown_user() {
    authenticate(current);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_unfollow_user() {
    authenticate(current);
    FollowRelation relation = new FollowRelation(current.getId(), target.getId());
    when(userRepository.findRelation(eq(current.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(current)))
        .thenReturn(Optional.of(profileDataOf(target, false)));

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
                target.getUsername()),
            "data.unfollowUser.profile.following");

    verify(userRepository).removeRelation(eq(relation));
    assertThat(following).isFalse();
  }

  @Test
  void should_return_error_when_unfollowing_user_that_is_not_followed() {
    authenticate(current);
    when(userRepository.findRelation(eq(current.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { unfollowUser(username: \"%s\") { profile { username } } }",
                target.getUsername()));

    assertFailedWith(result, ResourceNotFoundException.class);
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  void should_not_unfollow_user_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { unfollowUser(username: \"%s\") { profile { username } } }",
                target.getUsername()));

    assertFailedWith(result, AuthenticationException.class);
    verify(userRepository, never()).removeRelation(any());
  }

  private ProfileData profileDataOf(User user, boolean following) {
    return new ProfileData(
        user.getId(), user.getUsername(), user.getBio(), user.getImage(), following);
  }
}
