package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
public class RelationMutationTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_follow_user_success() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    User targetUser = new User("target@test.com", targetUsername, "123", "", defaultAvatar);
    ProfileData profileData = new ProfileData(targetUser.getId(), targetUsername, "Target bio", defaultAvatar, true);

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(targetUsername), eq(user))).thenReturn(Optional.of(profileData));

    String mutation =
        "mutation { followUser(username: \"" + targetUsername + "\") { profile { username following } } }";

    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.username");
    Boolean resultFollowing =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.following");

    assertThat(resultUsername).isEqualTo(targetUsername);
    assertThat(resultFollowing).isTrue();
  }

  @Test
  public void should_fail_follow_when_not_authenticated() {
    clearAuthentication();

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_follow_when_user_not_found() {
    setAuthenticatedUser(user);

    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_unfollow_user_success() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    User targetUser = new User("target@test.com", targetUsername, "123", "", defaultAvatar);
    ProfileData profileData = new ProfileData(targetUser.getId(), targetUsername, "Target bio", defaultAvatar, false);
    FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq(targetUsername), eq(user))).thenReturn(Optional.of(profileData));

    String mutation =
        "mutation { unfollowUser(username: \"" + targetUsername + "\") { profile { username following } } }";

    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.username");
    Boolean resultFollowing =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.following");

    assertThat(resultUsername).isEqualTo(targetUsername);
    assertThat(resultFollowing).isFalse();
  }

  @Test
  public void should_fail_unfollow_when_not_authenticated() {
    clearAuthentication();

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_unfollow_when_relation_not_found() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    User targetUser = new User("target@test.com", targetUsername, "123", "", defaultAvatar);

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"" + targetUsername + "\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
