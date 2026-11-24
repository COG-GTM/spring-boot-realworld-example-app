package io.spring.application.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** BCrypt rejects passwords longer than 72 UTF-8 bytes. */
@Constraint(validatedBy = PasswordLengthValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordLengthConstraint {
  String message() default "must be at most 72 bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
