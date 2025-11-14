package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NewArticleParamTest {

  @Test
  public void should_create_new_article_param_with_all_fields() {
    List<String> tags = Arrays.asList("java", "spring");
    NewArticleParam param =
        new NewArticleParam("Test Title", "Test Description", "Test Body", tags);

    assertThat(param.getTitle(), is("Test Title"));
    assertThat(param.getDescription(), is("Test Description"));
    assertThat(param.getBody(), is("Test Body"));
    assertThat(param.getTagList(), is(tags));
  }

  @Test
  public void should_create_new_article_param_with_builder() {
    List<String> tags = Arrays.asList("java", "spring");
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Builder Title")
            .description("Builder Description")
            .body("Builder Body")
            .tagList(tags)
            .build();

    assertThat(param.getTitle(), is("Builder Title"));
    assertThat(param.getDescription(), is("Builder Description"));
    assertThat(param.getBody(), is("Builder Body"));
    assertThat(param.getTagList(), is(tags));
  }

  @Test
  public void should_create_empty_new_article_param() {
    NewArticleParam param = new NewArticleParam();

    assertNotNull(param);
  }

  @Test
  public void should_create_new_article_param_without_tags() {
    NewArticleParam param = new NewArticleParam("Title", "Description", "Body", null);

    assertThat(param.getTitle(), is("Title"));
    assertThat(param.getDescription(), is("Description"));
    assertThat(param.getBody(), is("Body"));
    assertThat(param.getTagList(), is((List<String>) null));
  }
}
