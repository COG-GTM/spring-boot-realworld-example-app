package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class AuthenticationExceptionTest {

  @Test
  public void should_be_an_unchecked_exception_without_message_or_cause() {
    AuthenticationException exception = new AuthenticationException();

    assertTrue(exception instanceof RuntimeException);
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void should_be_thrown_when_there_is_no_current_user() {
    Optional<String> noCurrentUser = Optional.empty();

    AuthenticationException exception =
        assertThrows(
            AuthenticationException.class,
            () -> noCurrentUser.orElseThrow(AuthenticationException::new));

    assertNull(exception.getMessage());
  }

  @Test
  public void should_not_be_thrown_when_current_user_exists() {
    Optional<String> currentUser = Optional.of("user-id");

    assertEquals("user-id", currentUser.orElseThrow(AuthenticationException::new));
  }
}
