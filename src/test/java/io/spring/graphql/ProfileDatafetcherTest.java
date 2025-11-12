package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
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
      ProfileDatafetcher.class
    })
@TestPropertySource(properties = "dgs.graphql.schema-locations=classpath*:schema/**/*.graphqls")
public class ProfileDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @Test
  public void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(profileData));

    String query =
        "query Profile($username: String!) { "
            + "  profile(username: $username) { "
            + "    profile { "
            + "      username "
            + "      bio "
            + "      image "
            + "      following "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "testuser");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> profileResult = (Map<String, Object>) data.get("profile");
    assertNotNull(profileResult);
    Map<String, Object> profile = (Map<String, Object>) profileResult.get("profile");
    assertNotNull(profile);
    assertEquals("testuser", profile.get("username"));
    assertEquals("bio", profile.get("bio"));
    assertEquals("image", profile.get("image"));
    assertEquals(false, profile.get("following"));
  }

  @Test
  public void should_fail_query_profile_when_user_not_found() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any()))
        .thenReturn(Optional.empty());

    String query =
        "query Profile($username: String!) { "
            + "  profile(username: $username) { "
            + "    profile { "
            + "      username "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_query_profile_with_following_status() {
    ProfileData followingProfileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);
    when(profileQueryService.findByUsername(eq("testuser"), any()))
        .thenReturn(Optional.of(followingProfileData));

    String query =
        "query Profile($username: String!) { "
            + "  profile(username: $username) { "
            + "    profile { "
            + "      username "
            + "      following "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "testuser");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> profileResult = (Map<String, Object>) data.get("profile");
    assertNotNull(profileResult);
    Map<String, Object> profile = (Map<String, Object>) profileResult.get("profile");
    assertNotNull(profile);
    assertEquals(true, profile.get("following"));
  }
}
