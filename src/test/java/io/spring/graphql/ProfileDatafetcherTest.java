package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.netflix.graphql.dgs.exceptions.QueryException;

@SpringBootTest
public class ProfileDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "Test bio", "http://image.url");
    profileData = new ProfileData(user.getId(), "testuser", "Test bio", "http://image.url", false);

    setAnonymousAuthentication();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymousUser",
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  public void should_get_profile_by_username() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username bio image following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");
    String image = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.image");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(username).isEqualTo("testuser");
    assertThat(bio).isEqualTo("Test bio");
    assertThat(image).isEqualTo("http://image.url");
    assertThat(following).isFalse();
  }

  @Test
  public void should_get_profile_with_following_status_for_authenticated_user() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    ProfileData followingProfile = new ProfileData("other-id", "otheruser", "Other bio", "http://other.url", true);

    when(profileQueryService.findByUsername(eq("otheruser"), eq(user)))
        .thenReturn(Optional.of(followingProfile));

    String query = "{ profile(username: \"otheruser\") { profile { username following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(username).isEqualTo("otheruser");
    assertThat(following).isTrue();
  }

  @Test
  public void should_return_error_for_non_existent_profile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";

    assertThrows(QueryException.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    });
  }

  @Test
  public void should_get_profile_with_null_bio_and_image() {
    ProfileData profileWithNulls = new ProfileData(user.getId(), "testuser", null, null, false);

    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileWithNulls));

    String query = "{ profile(username: \"testuser\") { profile { username bio image following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");
    String image = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.image");

    assertThat(username).isEqualTo("testuser");
    assertThat(bio).isNull();
    assertThat(image).isNull();
  }

  @Test
  public void should_get_profile_for_unauthenticated_user_with_following_false() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username following } } }";

    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(following).isFalse();
  }

  @Test
  public void should_get_profile_with_different_users() {
    User anotherUser = new User("another@test.com", "anotheruser", "password", "Another bio", "http://another.url");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(anotherUser, null, new ArrayList<>()));

    ProfileData targetProfile = new ProfileData(user.getId(), "testuser", "Test bio", "http://image.url", false);

    when(profileQueryService.findByUsername(eq("testuser"), eq(anotherUser)))
        .thenReturn(Optional.of(targetProfile));

    String query = "{ profile(username: \"testuser\") { profile { username bio following } } }";

    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(username).isEqualTo("testuser");
    assertThat(following).isFalse();
  }
}
