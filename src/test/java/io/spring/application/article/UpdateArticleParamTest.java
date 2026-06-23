package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateArticleParamTest {

  private ValidatorFactory validatorFactory;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterEach
  public void tearDown() {
    validatorFactory.close();
  }

  @Test
  public void should_default_to_empty_strings_with_no_args_constructor() {
    UpdateArticleParam param = new UpdateArticleParam();
    assertThat(param.getTitle()).isEmpty();
    assertThat(param.getBody()).isEmpty();
    assertThat(param.getDescription()).isEmpty();
  }

  @Test
  public void should_expose_all_fields_through_all_args_constructor_and_getters() {
    UpdateArticleParam param = new UpdateArticleParam("new title", "new body", "new desc");
    assertThat(param.getTitle()).isEqualTo("new title");
    assertThat(param.getBody()).isEqualTo("new body");
    assertThat(param.getDescription()).isEqualTo("new desc");
  }

  @Test
  public void should_have_no_validation_constraints() {
    UpdateArticleParam param = new UpdateArticleParam("", "", "");
    assertThat(validator.validate(param)).isEmpty();

    UpdateArticleParam filled = new UpdateArticleParam("t", "b", "d");
    assertThat(validator.validate(filled)).isEmpty();
  }
}
