package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, MeDatafetcher.class})
public class MeDatafetcherTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private UserQueryService userQueryService;

  @MockBean
  private JwtService jwtService;

  private User user;
  private UserData userData;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "Test bio", "http://image.url/avatar.jpg");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());

    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymous", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldGetCurrentUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, Arrays.asList()));
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");

    String query = "{ me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo("user@example.com");
    assertThat(result.get("username")).isEqualTo("testuser");
  }

  @Test
  void shouldReturnNullWhenNotAuthenticated() {
    String query = "{ me { email username } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);

    assertThat(result).isNull();
  }
}
