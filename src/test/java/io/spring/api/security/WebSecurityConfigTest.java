package io.spring.api.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies the access rules declared by {@link WebSecurityConfig} against a throwaway controller
 * that mirrors the routes of the real API, so the assertions are about the security configuration
 * only and not about controller behaviour.
 */
@WebMvcTest(controllers = WebSecurityConfigTest.RoutesController.class)
@Import({WebSecurityConfig.class, WebSecurityConfigTest.RouteConfiguration.class})
public class WebSecurityConfigTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private User user;
  private String token;

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    token = "valid.jwt.token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
  }

  @Test
  public void should_permit_user_registration_and_login() throws Exception {
    mvc.perform(post("/users")).andExpect(status().isOk());
    mvc.perform(post("/users/login")).andExpect(status().isOk());
  }

  @Test
  public void should_permit_reading_articles_profiles_and_tags() throws Exception {
    mvc.perform(get("/articles")).andExpect(status().isOk());
    mvc.perform(get("/articles/some-slug")).andExpect(status().isOk());
    mvc.perform(get("/articles/some-slug/comments")).andExpect(status().isOk());
    mvc.perform(get("/profiles/johnjacob")).andExpect(status().isOk());
    mvc.perform(get("/tags")).andExpect(status().isOk());
  }

  @Test
  public void should_permit_graphql_endpoints() throws Exception {
    mvc.perform(get("/graphiql")).andExpect(status().isOk());
    mvc.perform(post("/graphql")).andExpect(status().isOk());
  }

  @Test
  public void should_permit_options_requests_on_protected_paths() throws Exception {
    mvc.perform(options("/user")).andExpect(status().isOk());
  }

  @Test
  public void should_require_authentication_for_the_article_feed() throws Exception {
    mvc.perform(get("/articles/feed")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_require_authentication_for_current_user_endpoints() throws Exception {
    mvc.perform(get("/user")).andExpect(status().isUnauthorized());
    mvc.perform(put("/user")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_require_authentication_for_writing_endpoints() throws Exception {
    mvc.perform(post("/articles")).andExpect(status().isUnauthorized());
    mvc.perform(put("/articles/some-slug")).andExpect(status().isUnauthorized());
    mvc.perform(delete("/articles/some-slug")).andExpect(status().isUnauthorized());
    mvc.perform(post("/articles/some-slug/comments")).andExpect(status().isUnauthorized());
    mvc.perform(post("/articles/some-slug/favorite")).andExpect(status().isUnauthorized());
    mvc.perform(post("/profiles/johnjacob/follow")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_reject_a_protected_request_carrying_an_unknown_token() throws Exception {
    when(jwtService.getSubFromToken(eq("garbage"))).thenReturn(Optional.empty());

    mvc.perform(get("/user").header("Authorization", "Token garbage"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void should_allow_a_protected_request_carrying_a_valid_token() throws Exception {
    mvc.perform(get("/user").header("Authorization", "Token " + token)).andExpect(status().isOk());
    mvc.perform(get("/articles/feed").header("Authorization", "Token " + token))
        .andExpect(status().isOk());
    mvc.perform(post("/articles").header("Authorization", "Token " + token))
        .andExpect(status().isOk());
  }

  @TestConfiguration
  static class RouteConfiguration {
    @Bean
    public RoutesController routesController() {
      return new RoutesController();
    }
  }

  @RestController
  static class RoutesController {

    @PostMapping("/users")
    public String createUser() {
      return "created";
    }

    @PostMapping("/users/login")
    public String login() {
      return "logged in";
    }

    @GetMapping({"/user", "/tags", "/graphiql"})
    public String read() {
      return "read";
    }

    @PutMapping("/user")
    public String updateUser() {
      return "updated";
    }

    @GetMapping({
      "/articles",
      "/articles/feed",
      "/articles/{slug}",
      "/articles/{slug}/comments",
      "/profiles/{username}"
    })
    public String readArticles() {
      return "read";
    }

    @PostMapping({
      "/articles",
      "/articles/{slug}/comments",
      "/articles/{slug}/favorite",
      "/profiles/{username}/follow",
      "/graphql"
    })
    public String write() {
      return "written";
    }

    @PutMapping("/articles/{slug}")
    public String updateArticle() {
      return "updated";
    }

    @DeleteMapping("/articles/{slug}")
    public String deleteArticle() {
      return "deleted";
    }
  }
}
