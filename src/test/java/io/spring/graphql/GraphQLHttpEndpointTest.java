package io.spring.graphql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.application.TagsQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class GraphQLHttpEndpointTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TagsQueryService tagsQueryService;

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_execute_query_over_http() throws Exception {
    Mockito.when(tagsQueryService.allTags()).thenReturn(List.of("tag1", "tag2"));

    mockMvc
        .perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"{ tags }\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tags[0]").value("tag1"))
        .andExpect(jsonPath("$.data.tags[1]").value("tag2"));
  }

  @Test
  public void should_serve_graphiql() throws Exception {
    MvcResult result = mockMvc.perform(get("/graphiql")).andReturn();
    int status = result.getResponse().getStatus();
    org.assertj.core.api.Assertions.assertThat(status).isBetween(200, 399);
    if (status >= 300) {
      String location = result.getResponse().getHeader("Location");
      org.assertj.core.api.Assertions.assertThat(location).isNotBlank();
      result = mockMvc.perform(get(location)).andExpect(status().isOk()).andReturn();
    }
    String body = result.getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(body).contains("https://esm.sh/graphiql@5.4.0");
    org.assertj.core.api.Assertions.assertThat(body)
        .doesNotContain("unpkg.com/graphiql/graphiql.min.js");
  }
}
