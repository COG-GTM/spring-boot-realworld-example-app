package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
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
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User currentUser;
  private User targetUser;
  private ProfileData targetProfileData;

  @BeforeEach
  public void setUp() {
    currentUser =
        new User(
            "current@example.com",
            "currentuser",
            "password",
            "current bio",
            "current image");
    targetUser =
        new User(
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
  public void should_get_profile_by_username() {
    when(profileQueryService.findByUsername(
            eq(targetUser.getUsername()), any()))
        .thenReturn(Optional.of(targetProfileData));

    String query =
        "query { profile(username: \""
            + targetUser.getUsername()
            + "\") { profile { username bio image following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
    assertNotNull(result);
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertNotNull(profile);
    assertEquals("targetuser", profile.get("username"));
    assertEquals("target bio", profile.get("bio"));
  }

  @Test
  public void should_get_profile_with_following_status() {
    ProfileData followingProfile =
        new ProfileData(
            targetUser.getId(),
            targetUser.getUsername(),
            targetUser.getBio(),
            targetUser.getImage(),
            true);
    when(profileQueryService.findByUsername(
            eq(targetUser.getUsername()), any()))
        .thenReturn(Optional.of(followingProfile));

    String query =
        "query { profile(username: \""
            + targetUser.getUsername()
            + "\") { profile { username following } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
    assertNotNull(result);
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertNotNull(profile);
    assertTrue((Boolean) profile.get("following"));
  }

  @Test
  public void should_fail_get_nonexistent_profile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query =
        "query { profile(username: \"nonexistent\") "
            + "{ profile { username } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
        });
  }

  @Test
  public void should_fail_profile_without_authentication() {
    SecurityContextHolder.clearContext();

    String query =
        "query { profile(username: \""
            + targetUser.getUsername()
            + "\") { profile { username bio } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
        });
  }

  @Test
  public void should_get_own_profile() {
    ProfileData ownProfile =
        new ProfileData(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getBio(),
            currentUser.getImage(),
            false);
    when(profileQueryService.findByUsername(
            eq(currentUser.getUsername()), any()))
        .thenReturn(Optional.of(ownProfile));

    String query =
        "query { profile(username: \""
            + currentUser.getUsername()
            + "\") { profile { username bio image } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
    assertNotNull(result);
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertNotNull(profile);
    assertEquals("currentuser", profile.get("username"));
    assertEquals("current bio", profile.get("bio"));
  }
}
