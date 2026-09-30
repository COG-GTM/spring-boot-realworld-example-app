package io.spring.graphql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.login.max-failures-per-account=2")
public class UserMutationLoginThrottlingTest {
  @Autowired private MockMvc mvc;

  @Test
  public void should_throttle_graphql_login_mutation_per_account() throws Exception {
    String email = "graphql-" + UUID.randomUUID() + "@example.com";

    for (int i = 0; i < 2; i++) {
      login(email, "10.5.0." + i)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.errors[0].message").value("invalid email or password"))
          .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAUTHENTICATED"));
    }

    login(email, "10.5.1.1")
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.errors[0].message").value("too many login attempts, try again later"))
        .andExpect(jsonPath("$.errors[0].extensions.errorDetail").value("ENHANCE_YOUR_CALM"))
        .andExpect(jsonPath("$.errors[0].extensions.retryAfterSeconds").isNumber());
  }

  private ResultActions login(String email, String remoteAddr) throws Exception {
    String body =
        "{\"query\":\"mutation { login(email: \\\""
            + email
            + "\\\", password: \\\"wrong\\\") { user { email } } }\"}";
    return mvc.perform(
        post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(
                request -> {
                  request.setRemoteAddr(remoteAddr);
                  return request;
                }));
  }
}
