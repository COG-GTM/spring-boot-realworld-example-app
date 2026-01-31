package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class RelationMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User targetUser;
  private ProfileData targetProfileData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@test.com", "currentuser", "password", "bio", "image");
    targetUser = new User("target@test.com", "targetuser", "password", "target bio", "target image");

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));

    targetProfileData =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            true);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_follow_user() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(targetProfileData));

    String mutation =
        "mutation { followUser(username: \"targetuser\") { profile { username following } } }";

    ExecutionResult result = dgsQueryExecutor.execute(mutation);
    Map<String, Object> data = result.getData();
    Map<String, Object> followUser = (Map<String, Object>) data.get("followUser");
    Map<String, Object> profile = (Map<String, Object>) followUser.get("profile");
    String username = (String) profile.get("username");
    Boolean following = (Boolean) profile.get("following");

    assertThat(username).isEqualTo("targetuser");
    assertThat(following).isTrue();
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_unfollow_user() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());

    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));

    ProfileData unfollowedProfile =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(unfollowedProfile));

    String mutation =
        "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    ExecutionResult result = dgsQueryExecutor.execute(mutation);
    Map<String, Object> data = result.getData();
    Map<String, Object> unfollowUser = (Map<String, Object>) data.get("unfollowUser");
    Map<String, Object> profile = (Map<String, Object>) unfollowUser.get("profile");
    String username = (String) profile.get("username");

    assertThat(username).isEqualTo("targetuser");
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_fail_follow_without_auth() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { followUser(username: \"targetuser\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.username");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_fail_follow_nonexistent_user() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { followUser(username: \"nonexistent\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.username");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_fail_unfollow_without_existing_relation() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.username");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }
}
