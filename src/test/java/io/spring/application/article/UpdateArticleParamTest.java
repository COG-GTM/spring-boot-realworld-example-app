package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UpdateArticleParamTest {

  @Test
  public void should_create_with_default_values() {
    UpdateArticleParam param = new UpdateArticleParam();

    assertEquals("", param.getTitle());
    assertEquals("", param.getBody());
    assertEquals("", param.getDescription());
  }

  @Test
  public void should_create_with_all_args() {
    UpdateArticleParam param = new UpdateArticleParam("Title", "Body", "Desc");

    assertEquals("Title", param.getTitle());
    assertEquals("Body", param.getBody());
    assertEquals("Desc", param.getDescription());
  }

  @Test
  public void should_create_with_title_only() {
    UpdateArticleParam param = new UpdateArticleParam("Title", "", "");

    assertEquals("Title", param.getTitle());
    assertEquals("", param.getBody());
    assertEquals("", param.getDescription());
  }

  @Test
  public void should_create_with_body_only() {
    UpdateArticleParam param = new UpdateArticleParam("", "Body", "");

    assertEquals("", param.getTitle());
    assertEquals("Body", param.getBody());
    assertEquals("", param.getDescription());
  }

  @Test
  public void should_create_with_description_only() {
    UpdateArticleParam param = new UpdateArticleParam("", "", "Desc");

    assertEquals("", param.getTitle());
    assertEquals("", param.getBody());
    assertEquals("Desc", param.getDescription());
  }
}
