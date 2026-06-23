package io.spring.application.user;

import io.spring.core.user.UserRepository;
import java.lang.reflect.Field;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * Builds a Bean Validation {@link Validator} whose {@link ConstraintValidator}s receive a supplied
 * {@link UserRepository}. The duplicated-email/username and update validators rely on an injected
 * repository which the default validator factory cannot provide, so tests inject a mock here.
 */
final class ValidatorFactoryHelper {

  private ValidatorFactoryHelper() {}

  static Validator validatorWith(UserRepository userRepository) {
    ValidatorFactory factory =
        Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(new RepoAwareConstraintValidatorFactory(userRepository))
            .buildValidatorFactory();
    return factory.getValidator();
  }

  private static final class RepoAwareConstraintValidatorFactory
      implements ConstraintValidatorFactory {
    private final UserRepository userRepository;

    private RepoAwareConstraintValidatorFactory(UserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
      try {
        T instance = key.getDeclaredConstructor().newInstance();
        for (Field field : key.getDeclaredFields()) {
          if (UserRepository.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            field.set(instance, userRepository);
          }
        }
        return instance;
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Unable to instantiate validator " + key, e);
      }
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {}
  }
}
