package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SecurityUtilTest {

  private final User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_return_current_user_when_authenticated() {
    SecurityContextHelper.authenticate(user);

    Optional<User> current = SecurityUtil.getCurrentUser();

    assertTrue(current.isPresent());
    assertSame(user, current.get());
  }

  @Test
  public void should_return_empty_when_anonymous() {
    SecurityContextHelper.anonymous();

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }

  @Test
  public void should_return_empty_when_principal_is_null() {
    SecurityContextHelper.authenticateWithNullPrincipal();

    assertFalse(SecurityUtil.getCurrentUser().isPresent());
  }
}
