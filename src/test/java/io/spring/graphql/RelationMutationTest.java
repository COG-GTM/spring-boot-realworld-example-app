package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import java.util.Collections;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      RelationMutation.class
    })
@TestPropertySource(properties = "dgs.graphql.schema-locations=classpath*:schema/**/*.graphqls")
public class RelationMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    currentUser = new User("current@example.com", "currentuser", "password", "bio", "image");
    targetUser = new User("target@example.com", "targetuser", "password", "bio", "image");
    profileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), true);
    
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  @Test
  public void should_follow_user_successfully() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(profileData));

    String mutation =
        "mutation FollowUser($username: String!) { "
            + "  followUser(username: $username) { "
            + "    profile { "
            + "      username "
            + "      following "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "targetuser");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> followUser = (Map<String, Object>) data.get("followUser");
    assertNotNull(followUser);
    Map<String, Object> profile = (Map<String, Object>) followUser.get("profile");
    assertNotNull(profile);
    assertEquals("targetuser", profile.get("username"));
    assertEquals(true, profile.get("following"));

    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_follow_user_when_user_not_found() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation FollowUser($username: String!) { "
            + "  followUser(username: $username) { "
            + "    profile { "
            + "      username "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_unfollow_user_successfully() {
    FollowRelation followRelation = new FollowRelation(currentUser.getId(), targetUser.getId());
    ProfileData unfollowedProfileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
    
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq("targetuser"), any()))
        .thenReturn(Optional.of(unfollowedProfileData));

    String mutation =
        "mutation UnfollowUser($username: String!) { "
            + "  unfollowUser(username: $username) { "
            + "    profile { "
            + "      username "
            + "      following "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "targetuser");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> unfollowUser = (Map<String, Object>) data.get("unfollowUser");
    assertNotNull(unfollowUser);
    Map<String, Object> profile = (Map<String, Object>) unfollowUser.get("profile");
    assertNotNull(profile);
    assertEquals("targetuser", profile.get("username"));
    assertEquals(false, profile.get("following"));

    verify(userRepository).removeRelation(any(FollowRelation.class));
  }

  @Test
  public void should_fail_unfollow_user_when_user_not_found() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation UnfollowUser($username: String!) { "
            + "  unfollowUser(username: $username) { "
            + "    profile { "
            + "      username "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_fail_unfollow_user_when_not_following() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation UnfollowUser($username: String!) { "
            + "  unfollowUser(username: $username) { "
            + "    profile { "
            + "      username "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "targetuser");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }
}
