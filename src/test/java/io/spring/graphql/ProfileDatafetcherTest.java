package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class ProfileDatafetcherTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private UserRepository userRepository;

  private String defaultAvatar;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    user = new User("john@jacob.com", "johnjacob", "password", "I like to code", defaultAvatar);
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @Test
  public void should_query_profile_by_username() {
    String username = "johnjacob";

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileData));

    String query =
        String.format(
            "query { profile(username: \"%s\") { profile { username bio image following } } }",
            username);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.profile.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("bio")).isEqualTo(user.getBio());
    assertThat(result.get("image")).isEqualTo(user.getImage());
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  public void should_query_profile_with_following_status() {
    String username = "johnjacob";
    ProfileData followingProfileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(followingProfileData));

    String query =
        String.format(
            "query { profile(username: \"%s\") { profile { username bio image following } } }",
            username);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.profile.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("following")).isEqualTo(true);
  }

  @Test
  public void should_fail_to_query_nonexistent_profile() {
    String username = "nonexistentuser";

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.empty());

    String query =
        String.format(
            "query { profile(username: \"%s\") { profile { username bio image following } } }",
            username);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.profile.profile", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }

  @Test
  public void should_query_profile_with_null_bio() {
    String username = "johnjacob";
    ProfileData profileDataWithNullBio = new ProfileData(user.getId(), user.getUsername(), null, user.getImage(), false);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileDataWithNullBio));

    String query =
        String.format(
            "query { profile(username: \"%s\") { profile { username bio image following } } }",
            username);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.profile.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("bio")).isNull();
    assertThat(result.get("following")).isEqualTo(false);
  }

  @Test
  public void should_query_profile_with_null_image() {
    String username = "johnjacob";
    ProfileData profileDataWithNullImage = new ProfileData(user.getId(), user.getUsername(), user.getBio(), null, false);

    when(profileQueryService.findByUsername(eq(username), any())).thenReturn(Optional.of(profileDataWithNullImage));

    String query =
        String.format(
            "query { profile(username: \"%s\") { profile { username bio image following } } }",
            username);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.profile.profile", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo(username);
    assertThat(result.get("image")).isNull();
    assertThat(result.get("following")).isEqualTo(false);
  }
}
