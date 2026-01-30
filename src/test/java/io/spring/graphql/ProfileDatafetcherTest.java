package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
public class ProfileDatafetcherTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_get_profile_by_username() {
    ProfileData profileData = new ProfileData(user.getId(), username, "Test bio", defaultAvatar, false);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String query =
        "query { profile(username: \"" + username + "\") { profile { username bio image following } } }";

    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String resultBio =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");
    String resultImage =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.image");
    Boolean resultFollowing =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(resultUsername).isEqualTo(username);
    assertThat(resultBio).isEqualTo("Test bio");
    assertThat(resultImage).isEqualTo(defaultAvatar);
    assertThat(resultFollowing).isFalse();
  }

  @Test
  public void should_get_profile_with_following_true_when_authenticated() {
    setAuthenticatedUser(user);

    String targetUsername = "targetuser";
    ProfileData profileData = new ProfileData("target-id", targetUsername, "Target bio", defaultAvatar, true);

    when(profileQueryService.findByUsername(eq(targetUsername), eq(user))).thenReturn(Optional.of(profileData));

    String query =
        "query { profile(username: \"" + targetUsername + "\") { profile { username following } } }";

    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    Boolean resultFollowing =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertThat(resultUsername).isEqualTo(targetUsername);
    assertThat(resultFollowing).isTrue();
  }

  @Test
  public void should_fail_when_profile_not_found() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query = "query { profile(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
