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

public class DuplicatedEmailValidatorTest {

  @Test
  public void should_accept_null_and_empty_email() {
    UserRepository repository = mock(UserRepository.class);
    DuplicatedEmailValidator validator = validatorWith(repository);

    assertThat(validator.isValid(null, null), is(true));
    assertThat(validator.isValid("", null), is(true));
  }

  @Test
  public void should_reject_existing_email() {
    UserRepository repository = mock(UserRepository.class);
    when(repository.findByEmail("email")).thenReturn(Optional.of(new User()));
    DuplicatedEmailValidator validator = validatorWith(repository);

    assertThat(validator.isValid("email", null), is(false));
  }

  @Test
  public void should_accept_new_email() {
    UserRepository repository = mock(UserRepository.class);
    when(repository.findByEmail("email")).thenReturn(Optional.empty());
    DuplicatedEmailValidator validator = validatorWith(repository);

    assertThat(validator.isValid("email", null), is(true));
  }

  private DuplicatedEmailValidator validatorWith(UserRepository repository) {
    DuplicatedEmailValidator validator = new DuplicatedEmailValidator();
    ReflectionTestUtils.setField(validator, "userRepository", repository);
    return validator;
  }
}
