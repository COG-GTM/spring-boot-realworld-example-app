package io.spring.infrastructure.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultJwtServiceExtendedTest {

  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService =
        new DefaultJwtService(
            "123123123123123123123123123123123123123123123123123123123123", 3600);
  }

  @Test
  public void should_generate_different_tokens_for_different_users() {
    User user1 = new User("user1@test.com", "user1", "pass", "", "");
    User user2 = new User("user2@test.com", "user2", "pass", "", "");
    String token1 = jwtService.toToken(user1);
    String token2 = jwtService.toToken(user2);
    assertThat(token1, not(token2));
  }

  @Test
  public void should_parse_subject_correctly() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = jwtService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertThat(subject.isPresent(), is(true));
    assertThat(subject.get(), is(user.getId()));
  }

  @Test
  public void should_return_empty_for_null_token() {
    Optional<String> subject = jwtService.getSubFromToken(null);
    assertThat(subject.isPresent(), is(false));
  }

  @Test
  public void should_return_empty_for_empty_token() {
    Optional<String> subject = jwtService.getSubFromToken("");
    assertThat(subject.isPresent(), is(false));
  }

  @Test
  public void should_return_empty_for_malformed_token() {
    Optional<String> subject = jwtService.getSubFromToken("not.a.valid.jwt.token");
    assertThat(subject.isPresent(), is(false));
  }

  @Test
  public void should_generate_non_null_token() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = jwtService.toToken(user);
    assertThat(token, notNullValue());
    assertThat(token.isEmpty(), is(false));
  }

  @Test
  public void should_generate_token_with_three_parts() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = jwtService.toToken(user);
    String[] parts = token.split("\\.");
    assertThat(parts.length, is(3));
  }

  @Test
  public void should_return_empty_for_token_signed_with_different_key() {
    JwtService otherService =
        new DefaultJwtService(
            "999999999999999999999999999999999999999999999999999999999999", 3600);
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = otherService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertThat(subject.isPresent(), is(false));
  }

  @Test
  public void should_handle_expired_token() {
    JwtService shortLivedService =
        new DefaultJwtService(
            "123123123123123123123123123123123123123123123123123123123123", 0);
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = shortLivedService.toToken(user);
    Optional<String> subject = jwtService.getSubFromToken(token);
    assertThat(subject.isPresent(), is(false));
  }
}
