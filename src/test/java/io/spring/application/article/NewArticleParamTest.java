package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NewArticleParamTest {

  @Test
  public void should_expose_all_values_passed_to_all_args_constructor() {
    List<String> tags = Arrays.asList("java", "spring");

    NewArticleParam param = new NewArticleParam("title", "desc", "body", tags);

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getDescription()).isEqualTo("desc");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getTagList()).isEqualTo(tags);
  }

  @Test
  public void should_build_param_with_builder() {
    NewArticleParam param =
        NewArticleParam.builder().title("title").description("desc").body("body").build();

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getDescription()).isEqualTo("desc");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getTagList()).isNull();
    assertThat(NewArticleParam.builder().toString()).contains("title");
  }

  @Test
  public void should_have_null_fields_when_created_with_no_args_constructor() {
    NewArticleParam param = new NewArticleParam();

    assertThat(param.getTitle()).isNull();
    assertThat(param.getDescription()).isNull();
    assertThat(param.getBody()).isNull();
    assertThat(param.getTagList()).isNull();
  }
}
