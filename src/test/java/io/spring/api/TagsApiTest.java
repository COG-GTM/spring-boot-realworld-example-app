package io.spring.api;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
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

@WebMvcTest({TagsApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class TagsApiTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_all_tags() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags.size()", equalTo(2))
        .body("tags[0]", equalTo("java"))
        .body("tags[1]", equalTo("spring"));
  }

  @Test
  public void should_get_empty_tags() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", empty());
  }
}
