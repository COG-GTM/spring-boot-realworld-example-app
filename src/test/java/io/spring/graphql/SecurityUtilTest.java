package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest extends GraphQLTestBase {

  @Test
  void should_return_current_user_when_authenticated() {
    Optional<User> current = SecurityUtil.getCurrentUser();

    assertThat(current).isPresent();
    assertThat(current.get()).isEqualTo(user);
  }

  @Test
  void should_return_empty_when_anonymous() {
    anonymous();

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                null, null, AuthorityUtils.createAuthorityList("ROLE_USER")));

    assertThat(SecurityUtil.getCurrentUser()).isEmpty();
  }
}
