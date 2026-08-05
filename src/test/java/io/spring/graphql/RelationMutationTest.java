package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
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
public class RelationMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private final User target = new User("jane@jacob.com", "jane", "123", "jane bio", "jane.png");

  @Test
  void should_follow_user() {
    when(userRepository.findByUsername(eq("jane"))).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername(eq("jane"), eq(user)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "jane", "jane bio", "jane.png", true)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { followUser(username: \"jane\") { profile { username bio image following } }"
                + " }");

    assertThat(context.read("$.data.followUser.profile.username", String.class)).isEqualTo("jane");
    assertThat(context.read("$.data.followUser.profile.following", Boolean.class)).isTrue();

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
  }

  @Test
  void should_not_follow_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  void should_not_follow_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"jane\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
  }

  @Test
  void should_unfollow_user() {
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq("jane"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("jane"), eq(user)))
        .thenReturn(
            Optional.of(new ProfileData(target.getId(), "jane", "jane bio", "jane.png", false)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { unfollowUser(username: \"jane\") { profile { username following } } }");

    assertThat(context.read("$.data.unfollowUser.profile.following", Boolean.class)).isFalse();
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  void should_not_unfollow_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_not_unfollow_user_without_existing_relation() {
    when(userRepository.findByUsername(eq("jane"))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"jane\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  void should_not_unfollow_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"jane\") { profile { username } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
  }
}
