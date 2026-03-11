package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ProfileDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_get_profile_by_username() {
    ProfileData profileData =
        new ProfileData(user.getId(), username, "Test bio", defaultAvatar, false);
    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"" + username + "\") { profile { username bio image following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("bio")).isEqualTo("Test bio");
    assertThat(result.get("image")).isEqualTo(defaultAvatar);
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  public void should_return_error_for_nonexistent_profile() {
    when(profileQueryService.findByUsername(any(), any())).thenReturn(Optional.empty());

    String query = "query { profile(username: \"nonexistent\") { profile { username bio } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_show_following_status_when_authenticated() {
    setAuthenticatedUser(user);

    ProfileData profileData =
        new ProfileData("other-id", "otheruser", "Other bio", "http://other.com/image", true);
    when(profileQueryService.findByUsername(eq("otheruser"), eq(user))).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"otheruser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("otheruser");
    assertThat(result.get("following")).isEqualTo(true);
  }

  @Test
  public void should_get_profile_without_authentication() {
    clearAuthentication();

    ProfileData profileData =
        new ProfileData(user.getId(), username, "Test bio", defaultAvatar, false);
    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String query = "query { profile(username: \"" + username + "\") { profile { username bio } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
  }
}
