package io.spring.graphql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "rate-limit.auth.limit=2",
      "rate-limit.auth.window=60s",
      "rate-limit.article-creation.limit=1",
      "rate-limit.article-creation.window=1h"
    })
public class GraphQLRateLimitTest {
  private static final String LOGIN =
      "mutation { login(email: \"nobody@example.com\", password: \"wrong\") { user { email } } }";

  @Autowired private MockMvc mvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_rate_limit_auth_mutations_with_shared_bucket() throws Exception {
    String ip = "10.1.0.1";
    graphql(ip, LOGIN, null)
        .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAUTHENTICATED"));
    graphql(ip, createUser("gql-auth"), null)
        .andExpect(jsonPath("$.data.createUser.user.email").value("gql-auth@example.com"));

    graphql(ip, LOGIN, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAVAILABLE"))
        .andExpect(jsonPath("$.errors[0].extensions.errorDetail").value("ENHANCE_YOUR_CALM"))
        .andExpect(jsonPath("$.errors[0].extensions.retryAfter").isNumber())
        .andExpect(jsonPath("$.errors[0].path[0]").value("login"));

    graphql("10.1.0.2", LOGIN, null)
        .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAUTHENTICATED"));
  }

  @Test
  public void should_rate_limit_create_article_mutation_per_user() throws Exception {
    String body =
        graphql("10.1.1.1", createUser("gql-writer"), null)
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(body, "$.data.createUser.user.token");
    String createArticle =
        "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"})"
            + " { article { slug } } }";

    graphql("10.1.1.1", createArticle, token)
        .andExpect(jsonPath("$.data.createArticle.article.slug").value("t"));
    graphql("10.1.1.2", createArticle, token)
        .andExpect(jsonPath("$.errors[0].extensions.errorDetail").value("ENHANCE_YOUR_CALM"))
        .andExpect(jsonPath("$.errors[0].path[0]").value("createArticle"));
  }

  private static String createUser(String name) {
    return "mutation { createUser(input: {email: \""
        + name
        + "@example.com\", username: \""
        + name
        + "\", password: \"password\"}) { ... on UserPayload { user { email token } } } }";
  }

  private ResultActions graphql(String ip, String query, String token) throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("query", query);
    payload.put("variables", Collections.emptyMap());
    MockHttpServletRequestBuilder request =
        post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload))
            .with(
                r -> {
                  r.setRemoteAddr(ip);
                  return r;
                });
    if (token != null) {
      request.header("Authorization", "Token " + token);
    }
    return mvc.perform(request);
  }
}
