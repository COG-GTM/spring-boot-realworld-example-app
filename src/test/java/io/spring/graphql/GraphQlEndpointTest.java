package io.spring.graphql;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class GraphQlEndpointTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Test
  public void should_read_graphql_request_body() throws Exception {
    mockMvc
        .perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"{ tags }\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tags").exists());
  }

  @Test
  public void should_map_authentication_errors_to_unauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"query\": \"mutation { login(email: \\\"nobody@example.com\\\", password:"
                        + " \\\"wrong\\\") { user { username } } }\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors[0].extensions.errorType").value("UNAUTHENTICATED"));
  }

  @Test
  public void should_resolve_current_user_from_security_context() throws Exception {
    String name = "me-" + UUID.randomUUID();
    User user = new User(name + "@example.com", name, "123", "", "");
    userRepository.save(user);

    mockMvc
        .perform(
            post("/graphql")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList())))
                .header("Authorization", "Token a-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"{ me { username email } }\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.me.username").value(name))
        .andExpect(jsonPath("$.data.me.email").value(name + "@example.com"));
  }
}
