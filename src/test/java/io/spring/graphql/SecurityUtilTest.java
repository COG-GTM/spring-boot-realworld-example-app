package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest {

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  @Test
  void should_return_current_user_when_authenticated() {
    User user = new User("user@test.com", "user", "123", "bio", "image");
    GraphQLTestSecurity.login(user);

    Optional<User> current = SecurityUtil.getCurrentUser();

    assertTrue(current.isPresent());
    assertEquals(user, current.get());
  }

  @Test
  void should_return_empty_when_anonymous() {
    GraphQLTestSecurity.anonymous();

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  void should_return_empty_when_principal_is_null() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  void should_throw_when_no_authentication_present() {
    GraphQLTestSecurity.clear();

    assertThrows(NullPointerException.class, SecurityUtil::getCurrentUser);
  }
}
