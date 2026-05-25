package io.spring.api.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigTest {

  @Autowired private MockMvc mvc;

  @MockBean private JwtService jwtService;
  @MockBean private UserRepository userRepository;

  @Test
  void options_requests_are_permitted() throws Exception {
    mvc.perform(options("/articles"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void graphql_endpoint_is_public() throws Exception {
    mvc.perform(post("/graphql").content("{}").contentType("application/json"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void graphiql_endpoint_is_public() throws Exception {
    mvc.perform(get("/graphiql"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void get_articles_is_public() throws Exception {
    mvc.perform(get("/articles"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void get_articles_slug_is_public() throws Exception {
    mvc.perform(get("/articles/some-slug"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void get_articles_feed_requires_authentication() throws Exception {
    mvc.perform(get("/articles/feed")).andExpect(status().isUnauthorized());
  }

  @Test
  void post_users_is_public() throws Exception {
    mvc.perform(post("/users").content("{}").contentType("application/json"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void post_users_login_is_public() throws Exception {
    mvc.perform(post("/users/login").content("{}").contentType("application/json"))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }

  @Test
  void other_endpoints_require_authentication() throws Exception {
    mvc.perform(delete("/articles/some-slug")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticated_user_can_access_protected_endpoint() throws Exception {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    String token = "valid-token";

    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    mvc.perform(get("/articles/feed").header("Authorization", "Token " + token))
        .andExpect(status().is(org.hamcrest.Matchers.not(401)));
  }
}
