package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, MeDatafetcher.class})
public class MeDatafetcherTest {

  private static final String ME_QUERY = "{ me { email username token } }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private MeDatafetcher meDatafetcher;

  @MockBean private UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@test.com", "john", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_the_current_user_with_the_token_of_the_authorization_header() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "image")));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            ME_QUERY, Collections.emptyMap(), authorizationHeader("Token jwt-token"));

    assertThat(result.<String>read("data.me.email")).isEqualTo(user.getEmail());
    assertThat(result.<String>read("data.me.username")).isEqualTo(user.getUsername());
    assertThat(result.<String>read("data.me.token")).isEqualTo("jwt-token");
  }

  @Test
  public void should_return_null_me_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            ME_QUERY,
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeader("Token jwt-token"));

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.<java.util.Map<String, Object>>getData()).containsEntry("me", null);
  }

  @Test
  public void should_return_null_me_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            ME_QUERY,
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeader("Token jwt-token"));

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.<java.util.Map<String, Object>>getData()).containsEntry("me", null);
  }

  @Test
  public void should_report_error_when_the_authenticated_user_no_longer_exists() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            ME_QUERY,
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeader("Token jwt-token"));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_report_error_when_the_authorization_header_is_missing() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "image")));

    ExecutionResult result = dgsQueryExecutor.execute(ME_QUERY);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_build_user_payload_user_with_a_freshly_generated_token() {
    when(jwtService.toToken(eq(user))).thenReturn("generated-token");
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("generated-token");
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  private HttpHeaders authorizationHeader(String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", value);
    return headers;
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }
}
