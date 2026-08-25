package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateArticleInputTypeTest {
  private static UpdateArticleInput full() {
    return new UpdateArticleInput("new body", "new description", "new title");
  }

  @Test
  public void should_build_with_builder() {
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .body("new body")
            .description("new description")
            .title("new title")
            .build();

    assertThat(input.getBody()).isEqualTo("new body");
    assertThat(input.getDescription()).isEqualTo("new description");
    assertThat(input.getTitle()).isEqualTo("new title");
    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    UpdateArticleInput input = new UpdateArticleInput();

    assertThat(input.getBody()).isNull();
    assertThat(input.getDescription()).isNull();
    assertThat(input.getTitle()).isNull();
  }

  @Test
  public void should_apply_setters() {
    UpdateArticleInput input = new UpdateArticleInput();
    input.setBody("new body");
    input.setDescription("new description");
    input.setTitle("new title");

    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    UpdateArticleInput input = full();

    assertThat(input.getBody()).isEqualTo("new body");
    assertThat(input.getDescription()).isEqualTo("new description");
    assertThat(input.getTitle()).isEqualTo("new title");
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    UpdateArticleInput input = full();

    assertThat(input).isEqualTo(input).isEqualTo(full()).isNotEqualTo(null);
    assertThat(input.equals("not an input")).isFalse();
    assertThat(input.hashCode()).isEqualTo(full().hashCode());
    assertThat(input).isNotEqualTo(new UpdateArticleInput("other", "new description", "new title"));
    assertThat(input).isNotEqualTo(new UpdateArticleInput("new body", "other", "new title"));
    assertThat(input).isNotEqualTo(new UpdateArticleInput("new body", "new description", "other"));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("UpdateArticleInput{")
        .contains("body='new body'")
        .contains("description='new description'")
        .contains("title='new title'")
        .endsWith("}");
  }
}
