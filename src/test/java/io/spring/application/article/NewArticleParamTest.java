package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class NewArticleParamTest {

  @Test
  public void should_create_new_article_param_with_builder() {
    NewArticleParam param = NewArticleParam.builder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(Arrays.asList("java", "spring"))
        .build();
    
    assertThat(param.getTitle(), is("Test Title"));
    assertThat(param.getDescription(), is("Test Description"));
    assertThat(param.getBody(), is("Test Body"));
    assertThat(param.getTagList().size(), is(2));
    assertThat(param.getTagList().get(0), is("java"));
    assertThat(param.getTagList().get(1), is("spring"));
  }

  @Test
  public void should_create_new_article_param_with_constructor() {
    NewArticleParam param = new NewArticleParam("Test Title", "Test Description", "Test Body", Arrays.asList("java"));
    
    assertThat(param.getTitle(), is("Test Title"));
    assertThat(param.getDescription(), is("Test Description"));
    assertThat(param.getBody(), is("Test Body"));
    assertThat(param.getTagList().size(), is(1));
  }
}
