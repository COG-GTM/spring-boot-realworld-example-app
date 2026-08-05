package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class TagsApiTest {

  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_tags_success() {
    when(tagsQueryService.allTags()).thenReturn(asList("reactjs", "angularjs"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", hasSize(2))
        .body("tags", contains("reactjs", "angularjs"));

    verify(tagsQueryService).allTags();
  }

  @Test
  public void should_get_empty_tags_success() {
    when(tagsQueryService.allTags()).thenReturn(emptyList());

    RestAssuredMockMvc.when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", empty());
  }

  @Test
  public void should_get_tags_without_authorization_header() {
    when(tagsQueryService.allTags()).thenReturn(asList("reactjs"));

    given()
        .when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", contains("reactjs"));

    verifyNoInteractions(jwtService, userRepository);
  }

  @Test
  public void should_get_tags_with_invalid_token() {
    when(tagsQueryService.allTags()).thenReturn(asList("reactjs"));
    when(jwtService.getSubFromToken(eq("invalid-token"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token invalid-token")
        .when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", contains("reactjs"));
  }
}
