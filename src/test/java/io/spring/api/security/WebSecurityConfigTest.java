package io.spring.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@WebMvcTest(controllers = WebSecurityConfigTest.ProbeController.class)
@Import({WebSecurityConfig.class, WebSecurityConfigTest.ProbeController.class})
public class WebSecurityConfigTest {
  private static final String VALID_TOKEN = "valid-token";

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    Mockito.when(jwtService.getSubFromToken(VALID_TOKEN)).thenReturn(Optional.of(user.getId()));
    Mockito.when(jwtService.getSubFromToken("invalid-token")).thenReturn(Optional.empty());
    Mockito.when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
  }

  @Test
  public void public_get_endpoints_should_allow_anonymous_access() throws Exception {
    mvc.perform(get("/tags")).andExpect(status().isOk());
    mvc.perform(get("/articles")).andExpect(status().isOk());
    mvc.perform(get("/articles/some-slug")).andExpect(status().isOk());
    mvc.perform(get("/articles/some-slug/comments")).andExpect(status().isOk());
    mvc.perform(get("/profiles/someone")).andExpect(status().isOk());
  }

  @Test
  public void registration_and_login_should_allow_anonymous_access() throws Exception {
    mvc.perform(post("/users")).andExpect(status().isOk());
    mvc.perform(post("/users/login")).andExpect(status().isOk());
  }

  @Test
  public void graphql_endpoints_should_allow_anonymous_access() throws Exception {
    mvc.perform(post("/graphql")).andExpect(status().isOk());
    mvc.perform(get("/graphiql")).andExpect(status().isOk());
  }

  @Test
  public void feed_should_require_authentication_even_though_articles_are_public()
      throws Exception {
    mvc.perform(get("/articles/feed")).andExpect(status().isUnauthorized());
    mvc.perform(get("/articles/feed").header("Authorization", "Token " + VALID_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  public void write_endpoints_should_return_401_for_anonymous_users() throws Exception {
    mvc.perform(post("/articles")).andExpect(status().isUnauthorized());
    mvc.perform(put("/articles/some-slug")).andExpect(status().isUnauthorized());
    mvc.perform(delete("/articles/some-slug")).andExpect(status().isUnauthorized());
    mvc.perform(post("/articles/some-slug/favorite")).andExpect(status().isUnauthorized());
    mvc.perform(post("/articles/some-slug/comments")).andExpect(status().isUnauthorized());
    mvc.perform(post("/profiles/someone/follow")).andExpect(status().isUnauthorized());
  }

  @Test
  public void current_user_endpoints_should_return_401_for_anonymous_users() throws Exception {
    mvc.perform(get("/user")).andExpect(status().isUnauthorized());
    mvc.perform(put("/user")).andExpect(status().isUnauthorized());
  }

  @Test
  public void protected_endpoints_should_return_401_for_invalid_token() throws Exception {
    mvc.perform(get("/user").header("Authorization", "Token invalid-token"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/articles").header("Authorization", "Token invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void protected_endpoints_should_allow_valid_token_and_expose_principal() throws Exception {
    mvc.perform(get("/user").header("Authorization", "Token " + VALID_TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(user.getUsername()));
    mvc.perform(post("/articles").header("Authorization", "Token " + VALID_TOKEN))
        .andExpect(status().isOk());
    mvc.perform(delete("/articles/some-slug").header("Authorization", "Token " + VALID_TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  public void unknown_paths_should_require_authentication() throws Exception {
    mvc.perform(get("/some/unknown/path")).andExpect(status().isUnauthorized());
  }

  @Test
  public void cors_preflight_should_be_permitted_without_authentication() throws Exception {
    mvc.perform(
            options("/articles")
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "*"));
  }

  @Test
  public void should_be_stateless_and_not_create_session() throws Exception {
    mvc.perform(get("/user").header("Authorization", "Token " + VALID_TOKEN))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Set-Cookie"));
  }

  @RequestMapping
  @ResponseBody
  static class ProbeController {
    @GetMapping({
      "/tags",
      "/articles",
      "/articles/feed",
      "/articles/{slug}",
      "/articles/{slug}/comments",
      "/profiles/{username}",
      "/graphiql",
      "/some/unknown/path"
    })
    public String okGet() {
      return "ok";
    }

    @PostMapping({
      "/users",
      "/users/login",
      "/graphql",
      "/articles",
      "/articles/{slug}/favorite",
      "/articles/{slug}/comments",
      "/profiles/{username}/follow"
    })
    public String okPost() {
      return "ok";
    }

    @PutMapping({"/articles/{slug}", "/user"})
    public String okPut() {
      return "ok";
    }

    @DeleteMapping("/articles/{slug}")
    public String okDelete() {
      return "ok";
    }

    @GetMapping("/user")
    public String currentUser(@AuthenticationPrincipal User user) {
      return user.getUsername();
    }
  }
}
