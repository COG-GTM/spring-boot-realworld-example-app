package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class UpdateUserParamTest {

  @Test
  void builder_applies_empty_string_defaults() {
    UpdateUserParam param = UpdateUserParam.builder().build();

    assertEquals("", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  void builder_sets_all_fields_and_exposes_to_string() {
    UpdateUserParam.UpdateUserParamBuilder builder =
        UpdateUserParam.builder()
            .email("e@test.com")
            .password("pass")
            .username("user")
            .bio("bio")
            .image("img");

    assertNotNull(builder.toString());

    UpdateUserParam param = builder.build();

    assertEquals("e@test.com", param.getEmail());
    assertEquals("pass", param.getPassword());
    assertEquals("user", param.getUsername());
    assertEquals("bio", param.getBio());
    assertEquals("img", param.getImage());
  }

  @Test
  void all_args_constructor_assigns_fields_in_order() {
    UpdateUserParam param = new UpdateUserParam("e@test.com", "pass", "user", "bio", "img");

    assertEquals("e@test.com", param.getEmail());
    assertEquals("pass", param.getPassword());
    assertEquals("user", param.getUsername());
    assertEquals("bio", param.getBio());
    assertEquals("img", param.getImage());
  }

  @Test
  void no_args_constructor_applies_builder_default_empty_strings() {
    UpdateUserParam param = new UpdateUserParam();

    assertEquals("", param.getEmail());
    assertEquals("", param.getPassword());
    assertEquals("", param.getUsername());
    assertEquals("", param.getBio());
    assertEquals("", param.getImage());
  }

  @Test
  void valid_email_passes_and_malformed_email_fails_validation() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    assertTrue(validator.validate(UpdateUserParam.builder().email("e@test.com").build()).isEmpty());
    assertFalse(validator.validate(UpdateUserParam.builder().email("bad").build()).isEmpty());
  }
}
