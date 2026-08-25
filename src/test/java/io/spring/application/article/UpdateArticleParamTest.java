package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateArticleParamTest {

  @Test
  public void should_default_all_fields_to_empty_string() {
    UpdateArticleParam param = new UpdateArticleParam();

    assertThat(param.getTitle()).isEmpty();
    assertThat(param.getBody()).isEmpty();
    assertThat(param.getDescription()).isEmpty();
  }

  @Test
  public void should_expose_values_in_title_body_description_order() {
    UpdateArticleParam param = new UpdateArticleParam("title", "body", "desc");

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getDescription()).isEqualTo("desc");
  }
}
