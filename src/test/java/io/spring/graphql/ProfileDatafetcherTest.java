package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "Test bio", "http://image.url");
    profileData =
        new ProfileData(
            user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_profile_by_username() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username bio image following } } }";

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");

    assert username.equals("testuser");
    assert bio.equals("Test bio");
  }

  @Test
  public void should_get_profile_with_following_status_when_authenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    ProfileData followingProfile =
        new ProfileData(
            "other-id", "otheruser", "Other bio", "http://other.image", true);

    when(profileQueryService.findByUsername(eq("otheruser"), eq(user)))
        .thenReturn(Optional.of(followingProfile));

    String query = "{ profile(username: \"otheruser\") { profile { username following } } }";

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assert username.equals("otheruser");
    assert following;
  }

  @Test
  public void should_fail_get_nonexistent_profile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_get_profile_without_authentication() {
    when(profileQueryService.findByUsername(eq("testuser"), eq(null)))
        .thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username bio } } }";

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");

    assert username.equals("testuser");
  }
}
