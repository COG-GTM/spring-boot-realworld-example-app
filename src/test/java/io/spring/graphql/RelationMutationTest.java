package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RelationMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_follow_user_when_authenticated() {
    setAuthenticatedUser(user);

    User targetUser = new User("target@test.com", "targetuser", "123", "Target bio", "http://target.com/image");
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));

    ProfileData profileData =
        new ProfileData(targetUser.getId(), "targetuser", "Target bio", "http://target.com/image", true);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(user))).thenReturn(Optional.of(profileData));

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("targetuser");
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_follow_user_without_authentication() {
    clearAuthentication();

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username following } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_follow_nonexistent_user() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username following } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_unfollow_user_when_authenticated() {
    setAuthenticatedUser(user);

    User targetUser = new User("target@test.com", "targetuser", "123", "Target bio", "http://target.com/image");
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));

    FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.of(followRelation));

    ProfileData profileData =
        new ProfileData(targetUser.getId(), "targetuser", "Target bio", "http://target.com/image", false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(user))).thenReturn(Optional.of(profileData));

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("targetuser");
    verify(userRepository).removeRelation(eq(followRelation));
  }

  @Test
  public void should_fail_unfollow_user_without_authentication() {
    clearAuthentication();

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_unfollow_nonexistent_user() {
    setAuthenticatedUser(user);
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"nonexistent\") { profile { username following } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_unfollow_user_not_following() {
    setAuthenticatedUser(user);

    User targetUser = new User("target@test.com", "targetuser", "123", "Target bio", "http://target.com/image");
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
