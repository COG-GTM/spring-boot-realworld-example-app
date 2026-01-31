package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RelationMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User targetUser;
  private ProfileData targetProfileData;

  @BeforeEach
  public void setUp() {
    currentUser = new User(
        "current@example.com",
        "currentuser",
        "password",
        "bio",
        "image");
    targetUser = new User(
        "target@example.com",
        "targetuser",
        "password",
        "target bio",
        "target image");

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null));

    targetProfileData =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            false);
  }

  @Test
  public void should_follow_user_success() {
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    ProfileData followingProfile =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            true);
    when(profileQueryService.findByUsername(
            eq(targetUser.getUsername()), eq(currentUser)))
        .thenReturn(Optional.of(followingProfile));

    String mutation =
        "mutation { followUser(username: \""
            + targetUser.getUsername()
            + "\") { profile { username following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.followUser");
    assertNotNull(result);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_unfollow_user_success() {
    FollowRelation relation =
        new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(
            eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(
            eq(targetUser.getUsername()), eq(currentUser)))
        .thenReturn(Optional.of(targetProfileData));

    String mutation =
        "mutation { unfollowUser(username: \""
            + targetUser.getUsername()
            + "\") { profile { username following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.unfollowUser");
    assertNotNull(result);
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  public void should_fail_follow_without_authentication() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { followUser(username: \""
            + targetUser.getUsername()
            + "\") { profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.followUser");
        });
  }

  @Test
  public void should_fail_unfollow_without_authentication() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { unfollowUser(username: \""
            + targetUser.getUsername()
            + "\") { profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.unfollowUser");
        });
  }

  @Test
  public void should_fail_follow_nonexistent_user() {
    when(userRepository.findByUsername(eq("nonexistent")))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { followUser(username: \"nonexistent\") "
            + "{ profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.followUser");
        });
  }

  @Test
  public void should_fail_unfollow_nonexistent_user() {
    when(userRepository.findByUsername(eq("nonexistent")))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { unfollowUser(username: \"nonexistent\") "
            + "{ profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.unfollowUser");
        });
  }

  @Test
  public void should_fail_unfollow_when_not_following() {
    when(userRepository.findByUsername(eq(targetUser.getUsername())))
        .thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(
            eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { unfollowUser(username: \""
            + targetUser.getUsername()
            + "\") { profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.unfollowUser");
        });
  }
}
