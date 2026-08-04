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
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
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

  private final User current = new User("me@example.com", "me", "123", "", "");
  private final User target = new User("t@example.com", "targetuser", "123", "t bio", "t.png");

  @Test
  void should_follow_user_and_persist_relation() {
    authenticate(current);
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("targetuser"), eq(current)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "targetuser", "t bio", "t.png", true)));

    String query =
        "mutation { followUser(username: \"targetuser\") { profile { username following } } }";

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.followUser.profile.username");
    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.followUser.profile.following");

    assertThat(username).isEqualTo("targetuser");
    assertThat(following).isTrue();

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository, org.mockito.Mockito.atLeastOnce()).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(current.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
  }

  @Test
  void should_error_when_following_unauthenticated() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"targetuser\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_error_when_following_missing_user() {
    authenticate(current);
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_unfollow_user_and_remove_relation() {
    authenticate(current);
    FollowRelation relation = new FollowRelation(current.getId(), target.getId());
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(current.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("targetuser"), eq(current)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "targetuser", "t bio", "t.png", false)));

    String query =
        "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.unfollowUser.profile.following");

    assertThat(following).isFalse();
    verify(userRepository, org.mockito.Mockito.atLeastOnce()).removeRelation(eq(relation));
  }

  @Test
  void should_error_when_unfollowing_without_existing_relation() {
    authenticate(current);
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(current.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).removeRelation(any());
  }
}
