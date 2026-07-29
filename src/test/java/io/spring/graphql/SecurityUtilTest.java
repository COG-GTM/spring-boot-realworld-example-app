package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_current_user_from_authentication_principal() {
    User user = new User("john@test.com", "john", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertThat(currentUser).isPresent();
    assertThat(currentUser.get()).isSameAs(user);
  }

  @Test
  public void should_return_empty_for_anonymous_authentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  public void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  public void should_throw_npe_when_there_is_no_authentication() {
    // documents current behaviour: an empty security context (authentication == null) is not
    // handled by getCurrentUser and blows up instead of returning Optional.empty()
    SecurityContextHolder.clearContext();

    assertThatThrownBy(SecurityUtil::getCurrentUser).isInstanceOf(NullPointerException.class);
  }
}
