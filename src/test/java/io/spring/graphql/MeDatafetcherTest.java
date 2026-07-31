package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, MeDatafetcher.class, ProfileDatafetcher.class})
class MeDatafetcherTest extends GraphQLTestBase {

  private static final String TOKEN = "jwt-token";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private JwtService jwtService;

  private User user;
  private HttpHeaders headers;

  @BeforeEach
  void setUp() {
    user = userFixture("john");
    headers = new HttpHeaders();
    headers.add("Authorization", "Token " + TOKEN);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getBio(),
                    user.getImage())));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    user.getId(), user.getUsername(), user.getBio(), user.getImage(), false)));
  }

  @Test
  void should_return_current_user_with_token_from_authorization_header() {
    authenticate(user);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { email username token profile { username bio } } }",
            Collections.emptyMap(),
            headers);

    assertThat(context.read("data.me.email", String.class)).isEqualTo(user.getEmail());
    assertThat(context.read("data.me.username", String.class)).isEqualTo(user.getUsername());
    assertThat(context.read("data.me.token", String.class)).isEqualTo(TOKEN);
    assertThat(context.read("data.me.profile.username", String.class))
        .isEqualTo(user.getUsername());
  }

  @Test
  void should_return_null_for_anonymous_user() {
    anonymous();

    Object me = dgsQueryExecutor.executeAndExtractJsonPath("{ me { email } }", "data.me", headers);

    assertThat(me).isNull();
  }

  @Test
  void should_return_error_when_current_user_is_missing_in_read_model() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ me { email } }", Collections.emptyMap(), Collections.emptyMap(), headers);

    assertFailedWith(result, ResourceNotFoundException.class);
  }
}
