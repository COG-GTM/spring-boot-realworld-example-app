package io.spring.api;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagsApi.class)
@AutoConfigureMockMvc(addFilters = false)
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
  public void should_return_all_tags_under_tags_root() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring", "graphql"));

    RestAssuredMockMvc.when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body("tags", hasSize(3))
        .body("tags[0]", equalTo("java"))
        .body("tags[1]", equalTo("spring"))
        .body("tags[2]", equalTo("graphql"));

    verify(tagsQueryService).allTags();
  }

  @Test
  public void should_return_empty_tags_array_when_no_tags_exist() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    RestAssuredMockMvc.when().get("/tags").then().statusCode(200).body("tags", empty());
  }
}
