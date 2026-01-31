package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class GraphQLTestHelper {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  protected MvcResult executeGraphQL(String query) throws Exception {
    return executeGraphQL(query, null, null);
  }

  protected MvcResult executeGraphQL(String query, Map<String, Object> variables)
      throws Exception {
    return executeGraphQL(query, variables, null);
  }

  protected MvcResult executeGraphQL(String query, Map<String, Object> variables, String token)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("query", query);
    if (variables != null) {
      body.put("variables", variables);
    }

    var requestBuilder =
        MockMvcRequestBuilders.post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));

    if (token != null) {
      requestBuilder.header("Authorization", "Token " + token);
    }

    return mockMvc.perform(requestBuilder).andReturn();
  }

  protected String getDataPath(MvcResult result, String path) throws Exception {
    String content = result.getResponse().getContentAsString();
    return JsonPath.read(content, "$.data." + path);
  }

  protected Object getDataObject(MvcResult result, String path) throws Exception {
    String content = result.getResponse().getContentAsString();
    return JsonPath.read(content, "$.data." + path);
  }

  protected String getErrorMessage(MvcResult result) throws Exception {
    String content = result.getResponse().getContentAsString();
    return JsonPath.read(content, "$.errors[0].message");
  }

  protected boolean hasErrors(MvcResult result) throws Exception {
    String content = result.getResponse().getContentAsString();
    try {
      Object errors = JsonPath.read(content, "$.errors");
      return errors != null;
    } catch (Exception e) {
      return false;
    }
  }
}
