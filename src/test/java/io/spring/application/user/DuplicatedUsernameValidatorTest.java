package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DuplicatedUsernameValidatorTest {

  @Test
  public void should_accept_null_and_empty_username() {
    UserRepository repository = mock(UserRepository.class);
    DuplicatedUsernameValidator validator = validatorWith(repository);

    assertThat(validator.isValid(null, null), is(true));
    assertThat(validator.isValid("", null), is(true));
  }

  @Test
  public void should_reject_existing_username() {
    UserRepository repository = mock(UserRepository.class);
    when(repository.findByUsername("username")).thenReturn(Optional.of(new User()));
    DuplicatedUsernameValidator validator = validatorWith(repository);

    assertThat(validator.isValid("username", null), is(false));
  }

  @Test
  public void should_accept_new_username() {
    UserRepository repository = mock(UserRepository.class);
    when(repository.findByUsername("username")).thenReturn(Optional.empty());
    DuplicatedUsernameValidator validator = validatorWith(repository);

    assertThat(validator.isValid("username", null), is(true));
  }

  private DuplicatedUsernameValidator validatorWith(UserRepository repository) {
    DuplicatedUsernameValidator validator = new DuplicatedUsernameValidator();
    ReflectionTestUtils.setField(validator, "userRepository", repository);
    return validator;
  }
}
