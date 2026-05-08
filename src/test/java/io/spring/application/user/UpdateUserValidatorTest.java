package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateUserValidatorTest {

  @Mock private UserRepository userRepository;
  private Validator validator;
  private ValidatorFactory factory;

  @BeforeEach
  public void setUp() {
    factory =
        Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(new MockedConstraintValidatorFactory(userRepository))
            .buildValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterEach
  public void tearDown() {
    if (factory != null) {
      factory.close();
    }
  }

  @Test
  public void should_be_valid_when_email_and_username_are_unused() {
    User target = new User("a@b.com", "alice", "secret", "", "");
    org.mockito.Mockito.when(userRepository.findByEmail("new@b.com"))
        .thenReturn(java.util.Optional.empty());
    org.mockito.Mockito.when(userRepository.findByUsername("alice2"))
        .thenReturn(java.util.Optional.empty());
    UpdateUserParam param = UpdateUserParam.builder().email("new@b.com").username("alice2").build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    Set<ConstraintViolation<UpdateUserCommand>> violations = validator.validate(command);
    assertTrue(violations.isEmpty());
  }

  @Test
  public void should_be_valid_when_email_belongs_to_target_user() {
    User target = new User("a@b.com", "alice", "secret", "", "");
    org.mockito.Mockito.when(userRepository.findByEmail("a@b.com"))
        .thenReturn(java.util.Optional.of(target));
    org.mockito.Mockito.when(userRepository.findByUsername("alice"))
        .thenReturn(java.util.Optional.of(target));
    UpdateUserParam param = UpdateUserParam.builder().email("a@b.com").username("alice").build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    Set<ConstraintViolation<UpdateUserCommand>> violations = validator.validate(command);
    assertTrue(violations.isEmpty());
  }

  @Test
  public void should_be_invalid_when_email_belongs_to_another_user() {
    User target = new User("a@b.com", "alice", "secret", "", "");
    User another = new User("b@b.com", "bob", "secret", "", "");
    org.mockito.Mockito.when(userRepository.findByEmail("b@b.com"))
        .thenReturn(java.util.Optional.of(another));
    org.mockito.Mockito.when(userRepository.findByUsername("alice"))
        .thenReturn(java.util.Optional.of(target));
    UpdateUserParam param = UpdateUserParam.builder().email("b@b.com").username("alice").build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    Set<ConstraintViolation<UpdateUserCommand>> violations = validator.validate(command);
    assertFalse(violations.isEmpty());
  }

  @Test
  public void should_be_invalid_when_username_belongs_to_another_user() {
    User target = new User("a@b.com", "alice", "secret", "", "");
    User another = new User("b@b.com", "bob", "secret", "", "");
    org.mockito.Mockito.when(userRepository.findByEmail("a@b.com"))
        .thenReturn(java.util.Optional.of(target));
    org.mockito.Mockito.when(userRepository.findByUsername("bob"))
        .thenReturn(java.util.Optional.of(another));
    UpdateUserParam param = UpdateUserParam.builder().email("a@b.com").username("bob").build();
    UpdateUserCommand command = new UpdateUserCommand(target, param);

    Set<ConstraintViolation<UpdateUserCommand>> violations = validator.validate(command);
    assertFalse(violations.isEmpty());
  }

  private static class MockedConstraintValidatorFactory
      implements javax.validation.ConstraintValidatorFactory {
    private final UserRepository userRepository;

    MockedConstraintValidatorFactory(UserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @Override
    public <T extends javax.validation.ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
      try {
        java.lang.reflect.Constructor<T> constructor = key.getDeclaredConstructor();
        constructor.setAccessible(true);
        T instance = constructor.newInstance();
        ReflectionTestUtils.setField(instance, "userRepository", userRepository);
        return instance;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void releaseInstance(javax.validation.ConstraintValidator<?, ?> instance) {}
  }
}
