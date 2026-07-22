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

public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedUsernameValidator validator;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void should_be_valid_when_username_is_null() {
    assertThat(validator.isValid(null, null), is(true));
  }

  @Test
  public void should_be_valid_when_username_is_empty() {
    assertThat(validator.isValid("", null), is(true));
  }

  @Test
  public void should_be_valid_when_username_not_taken() {
    when(userRepository.findByUsername(eq("free"))).thenReturn(Optional.empty());
    assertThat(validator.isValid("free", null), is(true));
  }

  @Test
  public void should_be_invalid_when_username_already_taken() {
    when(userRepository.findByUsername(eq("taken")))
        .thenReturn(Optional.of(new User("taken@test.com", "taken", "pass", "", "")));
    assertThat(validator.isValid("taken", null), is(false));
  }
}
