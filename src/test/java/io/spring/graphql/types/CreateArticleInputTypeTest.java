package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CreateArticleInputTypeTest {
  private static final List<String> TAGS = Arrays.asList("java", "spring");

  private static CreateArticleInput full() {
    return new CreateArticleInput("body", "description", TAGS, "title");
  }

  @Test
  public void should_build_with_builder() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .body("body")
            .description("description")
            .tagList(TAGS)
            .title("title")
            .build();

    assertThat(input.getBody()).isEqualTo("body");
    assertThat(input.getDescription()).isEqualTo("description");
    assertThat(input.getTagList()).containsExactly("java", "spring");
    assertThat(input.getTitle()).isEqualTo("title");
    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    CreateArticleInput input = new CreateArticleInput();

    assertThat(input.getBody()).isNull();
    assertThat(input.getDescription()).isNull();
    assertThat(input.getTagList()).isNull();
    assertThat(input.getTitle()).isNull();
  }

  @Test
  public void should_apply_setters() {
    CreateArticleInput input = new CreateArticleInput();
    input.setBody("body");
    input.setDescription("description");
    input.setTagList(TAGS);
    input.setTitle("title");

    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    CreateArticleInput input = full();

    assertThat(input.getBody()).isEqualTo("body");
    assertThat(input.getDescription()).isEqualTo("description");
    assertThat(input.getTagList()).isEqualTo(TAGS);
    assertThat(input.getTitle()).isEqualTo("title");
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    CreateArticleInput input = full();

    assertThat(input).isEqualTo(input).isEqualTo(full()).isNotEqualTo(null);
    assertThat(input.equals("not an input")).isFalse();
    assertThat(input.hashCode()).isEqualTo(full().hashCode());
    assertThat(input).isNotEqualTo(new CreateArticleInput("other", "description", TAGS, "title"));
    assertThat(input).isNotEqualTo(new CreateArticleInput("body", "other", TAGS, "title"));
    assertThat(input)
        .isNotEqualTo(
            new CreateArticleInput("body", "description", Collections.emptyList(), "title"));
    assertThat(input).isNotEqualTo(new CreateArticleInput("body", "description", TAGS, "other"));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("CreateArticleInput{")
        .contains("body='body'")
        .contains("description='description'")
        .contains("tagList='[java, spring]'")
        .contains("title='title'")
        .endsWith("}");
  }
}
