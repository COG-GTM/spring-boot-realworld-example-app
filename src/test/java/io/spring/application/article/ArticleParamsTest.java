package io.spring.application.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ArticleParamsTest {

  @Test
  public void should_build_new_article_param_with_builder() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("title")
            .description("description")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getDescription()).isEqualTo("description");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getTagList()).containsExactly("java");
  }

  @Test
  public void should_build_new_article_param_with_all_args_constructor() {
    NewArticleParam param =
        new NewArticleParam("title", "description", "body", Arrays.asList("java", "spring"));

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getDescription()).isEqualTo("description");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getTagList()).containsExactly("java", "spring");
  }

  @Test
  public void should_have_null_fields_for_default_new_article_param() {
    NewArticleParam param = new NewArticleParam();

    assertThat(param.getTitle()).isNull();
    assertThat(param.getDescription()).isNull();
    assertThat(param.getBody()).isNull();
    assertThat(param.getTagList()).isNull();
  }

  @Test
  public void should_default_update_article_param_fields_to_empty_string() {
    UpdateArticleParam param = new UpdateArticleParam();

    assertThat(param.getTitle()).isEmpty();
    assertThat(param.getBody()).isEmpty();
    assertThat(param.getDescription()).isEmpty();
  }

  @Test
  public void should_build_update_article_param_with_all_args_constructor() {
    UpdateArticleParam param = new UpdateArticleParam("title", "body", "description");

    assertThat(param.getTitle()).isEqualTo("title");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getDescription()).isEqualTo("description");
  }
}
