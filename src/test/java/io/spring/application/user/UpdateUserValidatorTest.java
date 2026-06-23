package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

class UpdateUserValidatorTest {

  private Set<String> violatedProperties(Set<ConstraintViolation<UpdateUserCommand>> violations) {
    return violations.stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  @Test
  void valid_when_email_and_username_are_not_used_by_anyone() {
    UserRepository repo = mock(UserRepository.class);
    when(repo.findByEmail("new@test.com")).thenReturn(Optional.empty());
    when(repo.findByUsername("new")).thenReturn(Optional.empty());
    User target = new User("t@test.com", "target", "p", "", "");
    UpdateUserParam param = UpdateUserParam.builder().email("new@test.com").username("new").build();
    Validator validator = ValidatorFactoryHelper.validatorWith(repo);

    assertTrue(validator.validate(new UpdateUserCommand(target, param)).isEmpty());
  }

  @Test
  void valid_when_email_and_username_belong_to_the_target_user() {
    UserRepository repo = mock(UserRepository.class);
    User target = new User("t@test.com", "target", "p", "", "");
    when(repo.findByEmail("t@test.com")).thenReturn(Optional.of(target));
    when(repo.findByUsername("target")).thenReturn(Optional.of(target));
    UpdateUserParam param =
        UpdateUserParam.builder().email("t@test.com").username("target").build();
    Validator validator = ValidatorFactoryHelper.validatorWith(repo);

    assertTrue(validator.validate(new UpdateUserCommand(target, param)).isEmpty());
  }

  @Test
  void invalid_when_email_and_username_are_taken_by_other_users() {
    UserRepository repo = mock(UserRepository.class);
    User target = new User("t@test.com", "target", "p", "", "");
    User other = new User("o@test.com", "other", "p", "", "");
    when(repo.findByEmail("o@test.com")).thenReturn(Optional.of(other));
    when(repo.findByUsername("other")).thenReturn(Optional.of(other));
    UpdateUserParam param = UpdateUserParam.builder().email("o@test.com").username("other").build();
    Validator validator = ValidatorFactoryHelper.validatorWith(repo);

    Set<String> properties =
        violatedProperties(validator.validate(new UpdateUserCommand(target, param)));

    assertTrue(properties.contains("email"));
    assertTrue(properties.contains("username"));
  }

  @Test
  void invalid_only_on_email_when_username_belongs_to_target() {
    UserRepository repo = mock(UserRepository.class);
    User target = new User("t@test.com", "target", "p", "", "");
    User other = new User("o@test.com", "other", "p", "", "");
    when(repo.findByEmail("o@test.com")).thenReturn(Optional.of(other));
    when(repo.findByUsername("target")).thenReturn(Optional.of(target));
    UpdateUserParam param =
        UpdateUserParam.builder().email("o@test.com").username("target").build();
    Validator validator = ValidatorFactoryHelper.validatorWith(repo);

    Set<String> properties =
        violatedProperties(validator.validate(new UpdateUserCommand(target, param)));

    assertTrue(properties.contains("email"));
    assertFalse(properties.contains("username"));
  }
}
