package io.spring.graphql;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public class GraphQLSchemaExposureTest {

  private static final String INTROSPECTION_QUERY =
      "{\"query\":\"{ __schema { queryType { name } } }\"}";

  private static ResultActions graphql(MockMvc mvc, String body) throws Exception {
    return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  class DefaultProfile {
    @Autowired private MockMvc mvc;

    @Test
    void graphiql_is_not_served_to_anonymous_users() throws Exception {
      mvc.perform(get("/graphiql")).andExpect(status().isUnauthorized());
      mvc.perform(get("/graphiql/index.html")).andExpect(status().isUnauthorized());
    }

    @Test
    void schema_json_is_not_served_to_anonymous_users() throws Exception {
      mvc.perform(get("/schema.json")).andExpect(status().isUnauthorized());
    }

    @Test
    void schema_introspection_is_rejected() throws Exception {
      graphql(mvc, INTROSPECTION_QUERY)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.__schema").doesNotExist())
          .andExpect(jsonPath("$.errors[0].message", containsString("__schema")));
    }

    @Test
    void type_introspection_is_rejected() throws Exception {
      graphql(mvc, "{\"query\":\"{ __type(name: \\\"User\\\") { fields { name } } }\"}")
          .andExpect(jsonPath("$.data.__type").doesNotExist())
          .andExpect(jsonPath("$.errors[0].message", containsString("__type")));
    }

    @Test
    void regular_queries_still_work_anonymously() throws Exception {
      graphql(mvc, "{\"query\":\"{ tags }\"}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.errors").doesNotExist())
          .andExpect(jsonPath("$.data.tags").isArray());
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @ActiveProfiles("dev")
  class DevProfile {
    @Autowired private MockMvc mvc;

    @Test
    void graphiql_is_served_to_anonymous_users() throws Exception {
      mvc.perform(get("/graphiql"))
          .andExpect(status().isOk())
          .andExpect(forwardedUrl("/graphiql/index.html"));
      mvc.perform(get("/graphiql/index.html"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("graphiql")));
    }

    @Test
    void schema_json_is_served_to_anonymous_users() throws Exception {
      mvc.perform(get("/schema.json"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("__schema")));
    }

    @Test
    void schema_introspection_is_allowed() throws Exception {
      graphql(mvc, INTROSPECTION_QUERY)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.__schema.queryType.name").value("Query"));
    }
  }
}
