package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedEmailValidator validator;

  private AutoCloseable mocks;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void should_be_valid_when_no_user_has_the_email() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

    assertThat(validator.isValid("john@example.com", null), is(true));

    verify(userRepository).findByEmail("john@example.com");
  }

  @Test
  public void should_be_invalid_when_a_user_already_has_the_email() {
    when(userRepository.findByEmail("john@example.com"))
        .thenReturn(Optional.of(new User("john@example.com", "john", "123", "", "")));

    assertThat(validator.isValid("john@example.com", null), is(false));
  }

  @Test
  public void should_be_valid_for_null_email_without_hitting_the_repository() {
    assertThat(validator.isValid(null, null), is(true));

    verify(userRepository, never()).findByEmail(any());
  }

  @Test
  public void should_be_valid_for_empty_email_without_hitting_the_repository() {
    assertThat(validator.isValid("", null), is(true));

    verify(userRepository, never()).findByEmail(any());
  }

  @Test
  public void should_query_the_repository_for_a_blank_email() {
    when(userRepository.findByEmail(" ")).thenReturn(Optional.empty());

    assertThat(validator.isValid(" ", null), is(true));

    verify(userRepository).findByEmail(" ");
  }
}
