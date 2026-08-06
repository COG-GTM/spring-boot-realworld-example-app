package io.spring.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
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

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_all_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body("tags", contains("java", "spring"));
  }

  @Test
  public void should_get_empty_tags_when_no_tag_exists() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when().get("/tags").then().statusCode(200).body("tags", empty());
  }

  @Test
  public void should_get_tags_with_an_unknown_token() {
    when(tagsQueryService.allTags()).thenReturn(Collections.singletonList("java"));
    when(jwtService.getSubFromToken(eq("unknown.jwt.token"))).thenReturn(Optional.empty());

    RestAssuredMockMvc.given()
        .header("Authorization", "Token unknown.jwt.token")
        .when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body("tags", contains("java"));
  }
}
