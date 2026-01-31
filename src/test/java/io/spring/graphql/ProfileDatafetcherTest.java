package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
@ActiveProfiles("test")
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "Test bio", "http://image.url");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldGetProfileByUsername() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username bio image following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("bio")).isEqualTo("Test bio");
    assertThat(result.get("image")).isEqualTo("http://image.url");
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  void shouldReturnErrorWhenProfileNotFound() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldGetProfileWithFollowingTrue() {
    ProfileData followingProfile = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(followingProfile));

    String query = "{ profile(username: \"testuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("following")).isEqualTo(true);
  }

  @Test
  void shouldGetProfileWithNullBioAndImage() {
    ProfileData profileWithNulls = new ProfileData(user.getId(), user.getUsername(), null, null, false);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileWithNulls));

    String query = "{ profile(username: \"testuser\") { profile { username bio image } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("bio")).isNull();
    assertThat(result.get("image")).isNull();
  }
}
