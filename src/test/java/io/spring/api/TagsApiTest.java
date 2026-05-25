package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
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
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "react"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", hasItems("java", "spring", "react"));
  }

  @Test
  public void should_get_empty_tags_list() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", hasSize(0));
  }
}
