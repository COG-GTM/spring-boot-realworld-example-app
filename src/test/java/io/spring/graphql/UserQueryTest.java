package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserQueryTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private UserQueryService userQueryService;

  @MockBean
  private JwtService jwtService;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User user;
  private UserData userData;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password", "Test bio", "http://image.url");
    userData = new UserData(user.getId(), "test@test.com", "testuser", "Test bio", "http://image.url");
    profileData = new ProfileData(user.getId(), "testuser", "Test bio", "http://image.url", false);

    setAnonymousAuthentication();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "key", "anonymousUser",
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  public void should_get_current_user_when_authenticated() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    String query = "{ me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    Map<String, Object> result = dgsQueryExecutor.executeAndGetDocumentContext(query, java.util.Collections.emptyMap(), headers).read("data.me");

    assertThat(result).isNotNull();
    assertThat(result.get("email")).isEqualTo("test@test.com");
    assertThat(result.get("username")).isEqualTo("testuser");
    assertThat(result.get("token")).isEqualTo("test-token");
  }

  @Test
  public void should_return_null_for_unauthenticated_user() {
    String query = "{ me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);

    assertThat(result).isNull();
  }

  @Test
  public void should_get_user_with_profile() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.of(userData));
    when(profileQueryService.findByUsername(eq("testuser"), eq(user)))
        .thenReturn(Optional.of(profileData));

    String query = "{ me { email username profile { username bio image following } } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    String email = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.email", headers);
    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.username", headers);
    String profileUsername = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.profile.username", headers);
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.profile.bio", headers);

    assertThat(email).isEqualTo("test@test.com");
    assertThat(username).isEqualTo("testuser");
    assertThat(profileUsername).isEqualTo("testuser");
    assertThat(bio).isEqualTo("Test bio");
  }

  @Test
  public void should_return_error_when_user_not_found_in_database() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>()));

    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.empty());

    String query = "{ me { email username } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    assertThrows(QueryException.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);
    });
  }

  @Test
  public void should_get_user_with_different_email_and_username() {
    User anotherUser = new User("another@test.com", "anotheruser", "password", "Another bio", "http://another.url");
    UserData anotherUserData = new UserData(anotherUser.getId(), "another@test.com", "anotheruser", "Another bio", "http://another.url");
    ProfileData anotherProfileData = new ProfileData(anotherUser.getId(), "anotheruser", "Another bio", "http://another.url", false);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(anotherUser, null, new ArrayList<>()));

    when(userQueryService.findById(eq(anotherUser.getId())))
        .thenReturn(Optional.of(anotherUserData));
    when(profileQueryService.findByUsername(eq("anotheruser"), eq(anotherUser)))
        .thenReturn(Optional.of(anotherProfileData));

    String query = "{ me { email username } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    String email = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.email", headers);
    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.username", headers);

    assertThat(email).isEqualTo("another@test.com");
    assertThat(username).isEqualTo("anotheruser");
  }

  @Test
  public void should_handle_anonymous_authentication() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", 
            java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    String query = "{ me { email username } }";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token test-token");

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);

    assertThat(result).isNull();
  }
}
