package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.UserQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, MeDatafetcher.class})
@Import({ProfileDatafetcher.class})
@ActiveProfiles("test")
public class MeDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  @MockBean private io.spring.application.ProfileQueryService profileQueryService;

  private User user;
  private UserData userData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "Test bio", "http://image.url");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @Test
  void shouldGetCurrentUser() {
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String query = "{ me { email username token profile { username bio image } } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, "Token test-token");

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
        query,
        "data.me",
        Collections.emptyMap(),
        Map.class,
        headers);

    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
  }

  @Test
  void shouldReturnNullWhenNotAuthenticated() {
    SecurityContextHolder.clearContext();

    String query = "{ me { email username } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getData().toString()).contains("me=null");
  }

  @Test
  void shouldGetUserWithToken() {
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String query = "{ me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, "Token my-jwt-token");

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
        query,
        "data.me",
        Collections.emptyMap(),
        Map.class,
        headers);

    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("token")).isEqualTo("my-jwt-token");
  }
}
