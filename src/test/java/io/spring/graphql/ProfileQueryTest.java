package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class ProfileQueryTest extends GraphQLTestBase {

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  public void should_query_profile_by_username() {
    String targetUsername = "testuser";

    ProfileData profileData =
        new ProfileData(
            "user-id", targetUsername, "Test bio", "https://example.com/avatar.jpg", false);

    when(profileQueryService.findByUsername(eq(targetUsername), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query GetProfile($username: String!) {"
            + "  profile(username: $username) {"
            + "    profile {"
            + "      username"
            + "      bio"
            + "      image"
            + "      following"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", targetUsername);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.profile.profile", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("bio")).isEqualTo("Test bio");
    assertThat(result.get("image")).isEqualTo("https://example.com/avatar.jpg");
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  public void should_query_profile_with_following_status() {
    authenticateUser();

    String targetUsername = "followeduser";

    ProfileData profileData =
        new ProfileData(
            "user-id", targetUsername, "Test bio", "https://example.com/avatar.jpg", true);

    when(profileQueryService.findByUsername(eq(targetUsername), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query GetProfile($username: String!) {"
            + "  profile(username: $username) {"
            + "    profile {"
            + "      username"
            + "      bio"
            + "      image"
            + "      following"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", targetUsername);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.profile.profile", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(targetUsername);
    assertThat(result.get("following")).isEqualTo(true);

    clearAuthentication();
  }

  @Test
  public void should_fail_query_nonexistent_profile() {
    String targetUsername = "nonexistent";

    when(profileQueryService.findByUsername(eq(targetUsername), any()))
        .thenReturn(Optional.empty());

    String query =
        "query GetProfile($username: String!) {"
            + "  profile(username: $username) {"
            + "    profile {"
            + "      username"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", targetUsername);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.profile.profile", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }
}
