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
import io.spring.TestHelper;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
public class RelationMutationTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private User target;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("follower");
    target = TestHelper.userFixture("target");
    authenticate(user);
  }

  private void stubProfile(boolean following) {
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(user)))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    target.getId(),
                    target.getUsername(),
                    target.getBio(),
                    target.getImage(),
                    following)));
  }

  @Test
  public void should_follow_a_user() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    stubProfile(true);

    Map<String, Object> profile =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { followUser(username: \""
                + target.getUsername()
                + "\") { profile { username bio image following } } }",
            "data.followUser.profile");

    assertThat(profile.get("username")).isEqualTo(target.getUsername());
    assertThat(profile.get("following")).isEqualTo(true);

    ArgumentCaptor<FollowRelation> captor = ArgumentCaptor.forClass(FollowRelation.class);
    verify(userRepository).saveRelation(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getTargetId()).isEqualTo(target.getId());
  }

  @Test
  public void should_not_follow_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_not_follow_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { followUser(username: \"any\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).saveRelation(any());
  }

  @Test
  public void should_unfollow_a_user() {
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.of(relation));
    stubProfile(false);

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { unfollowUser(username: \""
                + target.getUsername()
                + "\") { profile { username following } } }",
            "data.unfollowUser.profile.following");

    assertThat(following).isFalse();
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_not_unfollow_when_relation_does_not_exist() {
    when(userRepository.findByUsername(eq(target.getUsername()))).thenReturn(Optional.of(target));
    when(userRepository.findRelation(eq(user.getId()), eq(target.getId())))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \""
                + target.getUsername()
                + "\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).removeRelation(any());
  }

  @Test
  public void should_not_unfollow_unknown_user() {
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_not_unfollow_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { unfollowUser(username: \"any\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(userRepository, never()).removeRelation(any());
  }
}
