package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedEmailValidator validator;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void should_be_valid_when_email_is_null() {
    assertThat(validator.isValid(null, null), is(true));
  }

  @Test
  public void should_be_valid_when_email_is_empty() {
    assertThat(validator.isValid("", null), is(true));
  }

  @Test
  public void should_be_valid_when_email_not_taken() {
    when(userRepository.findByEmail(eq("free@test.com"))).thenReturn(Optional.empty());
    assertThat(validator.isValid("free@test.com", null), is(true));
  }

  @Test
  public void should_be_invalid_when_email_already_taken() {
    when(userRepository.findByEmail(eq("taken@test.com")))
        .thenReturn(Optional.of(new User("taken@test.com", "username", "pass", "", "")));
    assertThat(validator.isValid("taken@test.com", null), is(false));
  }
}
