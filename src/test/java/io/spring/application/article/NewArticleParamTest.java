package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class NewArticleParamTest {

  @Test
  public void should_create_with_builder() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList("tag1", "tag2"))
            .build();

    assertEquals("Title", param.getTitle());
    assertEquals("Desc", param.getDescription());
    assertEquals("Body", param.getBody());
    assertEquals(2, param.getTagList().size());
  }

  @Test
  public void should_create_with_all_args_constructor() {
    NewArticleParam param =
        new NewArticleParam("Title", "Desc", "Body", Arrays.asList("tag1"));

    assertEquals("Title", param.getTitle());
    assertEquals("Desc", param.getDescription());
    assertEquals("Body", param.getBody());
    assertEquals(1, param.getTagList().size());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    NewArticleParam param = new NewArticleParam();
    assertNull(param.getTitle());
    assertNull(param.getDescription());
    assertNull(param.getBody());
    assertNull(param.getTagList());
  }

  @Test
  public void should_create_with_empty_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(Collections.emptyList())
            .build();

    assertNotNull(param.getTagList());
    assertTrue(param.getTagList().isEmpty());
  }

  @Test
  public void should_create_with_null_tag_list() {
    NewArticleParam param =
        NewArticleParam.builder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(null)
            .build();

    assertNull(param.getTagList());
  }
}
