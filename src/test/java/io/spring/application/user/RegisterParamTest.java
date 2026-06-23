package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

class RegisterParamTest {

  private UserRepository repoWithNoExistingUsers() {
    UserRepository repo = mock(UserRepository.class);
    when(repo.findByEmail(any())).thenReturn(Optional.empty());
    when(repo.findByUsername(any())).thenReturn(Optional.empty());
    return repo;
  }

  private Set<String> violatedProperties(Set<ConstraintViolation<RegisterParam>> violations) {
    return violations.stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  @Test
  void should_expose_constructor_arguments_via_getters() {
    RegisterParam param = new RegisterParam("e@test.com", "user", "pass");

    assertEquals("e@test.com", param.getEmail());
    assertEquals("user", param.getUsername());
    assertEquals("pass", param.getPassword());
  }

  @Test
  void no_args_constructor_leaves_fields_null() {
    RegisterParam param = new RegisterParam();

    assertNull(param.getEmail());
    assertNull(param.getUsername());
    assertNull(param.getPassword());
  }

  @Test
  void valid_param_has_no_violations() {
    Validator validator = ValidatorFactoryHelper.validatorWith(repoWithNoExistingUsers());

    RegisterParam param = new RegisterParam("e@test.com", "user", "pass");

    assertTrue(validator.validate(param).isEmpty());
  }

  @Test
  void blank_fields_violate_not_blank_on_every_property() {
    Validator validator = ValidatorFactoryHelper.validatorWith(repoWithNoExistingUsers());

    Set<String> properties = violatedProperties(validator.validate(new RegisterParam("", "", "")));

    assertTrue(properties.contains("email"));
    assertTrue(properties.contains("username"));
    assertTrue(properties.contains("password"));
  }

  @Test
  void malformed_email_violates_email_constraint() {
    Validator validator = ValidatorFactoryHelper.validatorWith(repoWithNoExistingUsers());

    Set<String> properties =
        violatedProperties(validator.validate(new RegisterParam("not-an-email", "user", "pass")));

    assertTrue(properties.contains("email"));
  }

  @Test
  void duplicated_email_and_username_violate_constraints() {
    UserRepository repo = mock(UserRepository.class);
    User existing = new User("e@test.com", "user", "p", "", "");
    when(repo.findByEmail("e@test.com")).thenReturn(Optional.of(existing));
    when(repo.findByUsername("user")).thenReturn(Optional.of(existing));
    Validator validator = ValidatorFactoryHelper.validatorWith(repo);

    Set<String> properties =
        violatedProperties(validator.validate(new RegisterParam("e@test.com", "user", "pass")));

    assertTrue(properties.contains("email"));
    assertTrue(properties.contains("username"));
  }
}
