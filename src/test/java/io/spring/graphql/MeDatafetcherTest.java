package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, MeDatafetcher.class, ProfileDatafetcher.class})
public class MeDatafetcherTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("me");
  }

  @Test
  public void should_return_current_user_with_token_from_authorization_header() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.of(TestHelper.userDataFixture(user)));

    String email =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email username token } }", "data.me.email", authorizationHeader("jwt-token"));
    assertThat(email).isEqualTo(user.getEmail());

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email username token } }", "data.me.token", authorizationHeader("jwt-token"));
    assertThat(token).isEqualTo("jwt-token");
  }

  @Test
  public void should_resolve_profile_of_current_user() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.of(TestHelper.userDataFixture(user)));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    String username =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { profile { username bio image following } } }",
            "data.me.profile.username",
            authorizationHeader("jwt-token"));

    assertThat(username).isEqualTo(user.getUsername());
  }

  @Test
  public void should_return_null_when_user_is_anonymous() {
    authenticateAnonymously();

    Object me =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email } }", "data.me", authorizationHeader("jwt-token"));

    assertThat(me).isNull();
  }

  @Test
  public void should_fail_when_current_user_is_not_found() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ me { email } }",
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeader("jwt-token"));

    assertThat(result.getErrors()).isNotEmpty();
  }
}
