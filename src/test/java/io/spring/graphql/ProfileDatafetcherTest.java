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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
public class ProfileDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "Test bio", "http://image.url/avatar.jpg");
    profileData = new ProfileData(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getImage(),
        false);

    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymous", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
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

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("bio")).isEqualTo("Test bio");
    assertThat(result.get("image")).isEqualTo("http://image.url/avatar.jpg");
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  void shouldReturnErrorForNonExistentProfile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldGetProfileWithFollowingStatus() {
    ProfileData followingProfile = new ProfileData(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getImage(),
        true);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(followingProfile));

    String query = "{ profile(username: \"testuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("following")).isEqualTo(true);
  }
}
