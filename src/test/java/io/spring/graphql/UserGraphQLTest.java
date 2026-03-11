package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class, ProfileDatafetcher.class, RelationMutation.class})
@ActiveProfiles("test")
public class UserGraphQLTest extends GraphQLTestBase {

  @MockBean private UserService userService;

  @MockBean private UserQueryService userQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private PasswordEncoder passwordEncoder;

  @Test
  void should_create_user() {
    String newEmail = "newuser@test.com";
    String newUsername = "newuser";
    String password = "password123";

    User newUser = new User(newEmail, newUsername, password, "", "");

    when(userService.createUser(any())).thenReturn(newUser);
    when(jwtService.toToken(any(User.class))).thenReturn("generated-token");

    String mutation =
        "mutation { createUser(input: { email: \"" + newEmail + "\", username: \"" + newUsername + "\", password: \"" + password + "\" }) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser.user");

    assertThat(result.get("email")).isEqualTo(newEmail);
    assertThat(result.get("username")).isEqualTo(newUsername);
    assertThat(result.get("token")).isEqualTo("generated-token");
  }

  @Test
  void should_login_with_valid_credentials() {
    String loginEmail = "test@test.com";
    String loginPassword = "password123";

    User loginUser = new User(loginEmail, "testuser", loginPassword, "", "");

    when(userRepository.findByEmail(eq(loginEmail))).thenReturn(Optional.of(loginUser));
    when(passwordEncoder.matches(eq(loginPassword), eq(loginPassword))).thenReturn(true);
    when(jwtService.toToken(any(User.class))).thenReturn("login-token");

    String mutation =
        "mutation { login(email: \"" + loginEmail + "\", password: \"" + loginPassword + "\") { user { email username token } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login.user");

    assertThat(result.get("email")).isEqualTo(loginEmail);
    assertThat(result.get("token")).isEqualTo("login-token");
  }

  @Test
  void should_query_profile_by_username() {
    setAuthenticatedUser(user);

    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"" + username + "\") { profile { username bio image following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  void should_follow_user_when_authenticated() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    User targetUser = new User("target@test.com", targetUsername, "password", "", "");
    ProfileData profileData = new ProfileData(targetUser.getId(), targetUsername, "", "", true);

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq(targetUsername), eq(user))).thenReturn(Optional.of(profileData));

    String mutation = "mutation { followUser(username: \"" + targetUsername + "\") { profile { username following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile");

    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("following")).isEqualTo(true);
  }

  @Test
  void should_unfollow_user_when_authenticated() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    User targetUser = new User("target@test.com", targetUsername, "password", "", "");
    FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
    ProfileData profileData = new ProfileData(targetUser.getId(), targetUsername, "", "", false);

    when(userRepository.findByUsername(eq(targetUsername))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq(targetUsername), eq(user))).thenReturn(Optional.of(profileData));

    String mutation = "mutation { unfollowUser(username: \"" + targetUsername + "\") { profile { username following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile");

    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("following")).isEqualTo(false);
  }
}
