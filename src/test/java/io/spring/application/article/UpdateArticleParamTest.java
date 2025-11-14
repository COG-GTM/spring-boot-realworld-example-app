package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UpdateArticleParamTest {

  @Test
  public void should_create_update_article_param_with_all_fields() {
    UpdateArticleParam param = new UpdateArticleParam("New Title", "New Body", "New Description");

    assertThat(param.getTitle(), is("New Title"));
    assertThat(param.getBody(), is("New Body"));
    assertThat(param.getDescription(), is("New Description"));
  }

  @Test
  public void should_create_empty_update_article_param_with_defaults() {
    UpdateArticleParam param = new UpdateArticleParam();

    assertNotNull(param);
    assertThat(param.getTitle(), is(""));
    assertThat(param.getBody(), is(""));
    assertThat(param.getDescription(), is(""));
  }

  @Test
  public void should_handle_null_values() {
    UpdateArticleParam param = new UpdateArticleParam(null, null, null);

    assertThat(param.getTitle(), is((String) null));
    assertThat(param.getBody(), is((String) null));
    assertThat(param.getDescription(), is((String) null));
  }

  @Test
  public void should_handle_empty_strings() {
    UpdateArticleParam param = new UpdateArticleParam("", "", "");

    assertThat(param.getTitle(), is(""));
    assertThat(param.getBody(), is(""));
    assertThat(param.getDescription(), is(""));
  }
}
