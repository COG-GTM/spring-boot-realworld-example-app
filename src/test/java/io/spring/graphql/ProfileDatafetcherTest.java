package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
public class ProfileDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(testUser, null, null)
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnProfileByUsername() {
    String username = "testuser";
    ProfileData profileData = new ProfileData("user-id", username, "Test bio", "http://image.url", false);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ profile(username: \"" + username + "\") { profile { username bio image following } } }",
        "data.profile.profile"
    );

    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("bio")).isEqualTo("Test bio");
    assertThat(result.get("image")).isEqualTo("http://image.url");
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  void shouldReturnProfileWithFollowingTrue() {
    String username = "followeduser";
    ProfileData profileData = new ProfileData("user-id", username, "Bio", "http://image.url", true);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(
        "{ profile(username: \"" + username + "\") { profile { username following } } }",
        "data.profile.profile"
    );

    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("following")).isEqualTo(true);
  }
}
