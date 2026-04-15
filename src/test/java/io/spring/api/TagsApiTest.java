package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
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
public class TagsApiTest {
  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private UserReadService userReadService;

  @MockBean private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_tags() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "test"));

    given()
        .when()
        .get("/tags")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("tags", hasItems("java", "spring", "test"));
  }

  @Test
  public void should_get_empty_tags() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    given().when().get("/tags").prettyPeek().then().statusCode(200);
  }
}
