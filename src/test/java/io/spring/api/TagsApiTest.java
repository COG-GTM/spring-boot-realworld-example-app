package io.spring.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class TagsApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_tags_success() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "jpg"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body("tags", hasSize(3))
        .body("tags", contains("java", "spring", "jpg"));

    verify(tagsQueryService).allTags();
  }

  @Test
  public void should_get_empty_tags_success() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when().get("/tags").then().statusCode(200).body("tags", empty());
  }

  @Test
  public void should_get_tags_without_authentication() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Collections.singletonList("java"));

    RestAssuredMockMvc.given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body("tags", contains("java"));
  }
}
