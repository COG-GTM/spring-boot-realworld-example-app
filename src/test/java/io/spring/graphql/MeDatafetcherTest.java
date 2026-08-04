package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException;
import graphql.ExecutionResult;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      MeDatafetcher.class,
      ProfileDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class,
      RecordingExceptionHandler.class
    })
class MeDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;
  @MockBean private JwtService jwtService;
  // Collaborator of ProfileDatafetcher, imported to resolve User.profile.
  @MockBean private ProfileQueryService profileQueryService;

  private final User user =
      new User("me@example.com", "me", "123", "my bio", "https://image/me.png");

  @Test
  void should_return_current_user_with_token_from_header() {
    authenticate(user);
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "", "");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token-value");
    Map<String, Object> me =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email username token } }", "data.me", headers);

    assertThat(me.get("email")).isEqualTo(user.getEmail());
    assertThat(me.get("username")).isEqualTo(user.getUsername());
    assertThat(me.get("token")).isEqualTo("jwt-token-value");
  }

  @Test
  void should_resolve_profile_of_the_current_user() {
    authenticate(user);
    UserData userData =
        new UserData(user.getId(), user.getEmail(), user.getUsername(), "my bio", "i.png");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq("me"), eq(user)))
        .thenReturn(Optional.of(new ProfileData(user.getId(), "me", "my bio", "i.png", false)));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token-value");
    Map<String, Object> profile =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { profile { username bio image following } } }", "data.me.profile", headers);

    assertThat(profile.get("username")).isEqualTo("me");
    assertThat(profile.get("bio")).isEqualTo("my bio");
    assertThat(profile.get("image")).isEqualTo("i.png");
    assertThat(profile.get("following")).isEqualTo(false);
  }

  @Test
  void should_error_when_the_authorization_header_is_missing() {
    authenticate(user);

    ExecutionResult result = dgsQueryExecutor.execute("{ me { email username token } }");

    assertSingleErrorFrom(result, DgsInvalidInputArgumentException.class);
  }

  /**
   * {@code getMe} reads the token as {@code authorization.split(" ")[1]} without validating the
   * header, so a header carrying no scheme blows up instead of being rejected as unauthenticated.
   * Pinned here so a fix to that parsing is a deliberate change.
   */
  @Test
  void should_currently_fail_when_the_authorization_header_has_no_scheme() {
    authenticate(user);
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "", "");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "jwt-token-value");
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ me { email username token } }",
            Collections.emptyMap(),
            Collections.emptyMap(),
            headers);

    assertSingleErrorFrom(result, ArrayIndexOutOfBoundsException.class);
  }

  @Test
  void should_return_null_me_when_not_authenticated() {
    anonymous();

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token-value");
    Object me =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email username token } }", "data.me", headers);

    assertThat(me).isNull();
  }
}
