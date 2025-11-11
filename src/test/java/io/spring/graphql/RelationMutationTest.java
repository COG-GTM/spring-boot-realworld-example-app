package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      RelationMutation.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class RelationMutationTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private String defaultAvatar;
  private User currentUser;
  private User targetUser;
  private ProfileData targetProfileData;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    currentUser = new User("john@jacob.com", "johnjacob", "password", "bio", defaultAvatar);
    targetUser = new User("jane@doe.com", "janedoe", "password", "bio", defaultAvatar);
    targetProfileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_follow_user_successfully() {
    String targetUsername = "janedoe";

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));

    ProfileData followingProfileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), true);
    when(profileQueryService.findByUsername(eq(targetUsername), any())).thenReturn(Optional.of(followingProfileData));

    String query =
        String.format(
            "mutation { followUser(username: \"%s\") { profile { username bio image following } } }",
            targetUsername);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.followUser.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("following")).isEqualTo(true);

    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_unfollow_user_successfully() {
    String targetUsername = "janedoe";

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));

    FollowRelation followRelation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(followRelation));

    ProfileData unfollowingProfileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
    when(profileQueryService.findByUsername(eq(targetUsername), any())).thenReturn(Optional.of(unfollowingProfileData));

    String query =
        String.format(
            "mutation { unfollowUser(username: \"%s\") { profile { username bio image following } } }",
            targetUsername);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.unfollowUser.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("following")).isEqualTo(false);

    verify(userRepository).removeRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_to_follow_user_without_authentication() {
    String targetUsername = "janedoe";

    String query =
        String.format(
            "mutation { followUser(username: \"%s\") { profile { username following } } }",
            targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.followUser.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("AuthenticationException");
    }
  }

  @Test
  public void should_fail_to_unfollow_user_without_authentication() {
    String targetUsername = "janedoe";

    String query =
        String.format(
            "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
            targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.unfollowUser.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("AuthenticationException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_fail_to_follow_nonexistent_user() {
    String targetUsername = "nonexistentuser";

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { followUser(username: \"%s\") { profile { username following } } }",
            targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.followUser.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_fail_to_unfollow_nonexistent_user() {
    String targetUsername = "nonexistentuser";

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
            targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.unfollowUser.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_fail_to_unfollow_user_not_currently_following() {
    String targetUsername = "janedoe";

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { unfollowUser(username: \"%s\") { profile { username following } } }",
            targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.unfollowUser.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }
}
