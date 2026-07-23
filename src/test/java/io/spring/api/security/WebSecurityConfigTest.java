package io.spring.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.api.TagsApi;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagsApi.class)
@Import(WebSecurityConfig.class)
public class WebSecurityConfigTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;
  @MockBean private UserReadService userReadService;
  @MockBean private JwtService jwtService;
  @MockBean private TagsQueryService tagsQueryService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    Mockito.when(userRepository.findById(ArgumentMatchers.eq(user.getId())))
        .thenReturn(Optional.of(user));
    Mockito.when(jwtService.getSubFromToken(ArgumentMatchers.eq("valid-token")))
        .thenReturn(Optional.of(user.getId()));
    Mockito.when(jwtService.getSubFromToken(ArgumentMatchers.eq("invalid-token")))
        .thenReturn(Optional.empty());
  }

  @Test
  public void should_permit_options_requests_without_token() throws Exception {
    mvc.perform(options("/articles"))
        .andExpect(
            result ->
                org.junit.jupiter.api.Assertions.assertNotEquals(
                    401, result.getResponse().getStatus()));
  }

  @Test
  public void should_permit_graphql_endpoints_without_token() throws Exception {
    mvc.perform(get("/graphiql"))
        .andExpect(
            result ->
                org.junit.jupiter.api.Assertions.assertNotEquals(
                    401, result.getResponse().getStatus()));
    mvc.perform(post("/graphql"))
        .andExpect(
            result ->
                org.junit.jupiter.api.Assertions.assertNotEquals(
                    401, result.getResponse().getStatus()));
  }

  @Test
  public void should_permit_user_registration_and_login_without_token() throws Exception {
    mvc.perform(post("/users")).andExpect(status().isNotFound());
    mvc.perform(post("/users/login")).andExpect(status().isNotFound());
  }

  @Test
  public void should_permit_public_read_endpoints_without_token() throws Exception {
    mvc.perform(get("/articles")).andExpect(status().isNotFound());
    mvc.perform(get("/articles/some-slug")).andExpect(status().isNotFound());
    mvc.perform(get("/profiles/johnjacob")).andExpect(status().isNotFound());
    mvc.perform(get("/tags")).andExpect(status().isOk());
  }

  @Test
  public void should_reject_articles_feed_without_token() throws Exception {
    mvc.perform(get("/articles/feed")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_reject_protected_endpoints_without_token() throws Exception {
    mvc.perform(get("/user")).andExpect(status().isUnauthorized());
    mvc.perform(post("/articles")).andExpect(status().isUnauthorized());
  }

  @Test
  public void should_reject_protected_endpoints_with_invalid_token() throws Exception {
    mvc.perform(get("/user").header("Authorization", "Token invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void should_allow_protected_endpoints_with_valid_token() throws Exception {
    mvc.perform(get("/articles/feed").header("Authorization", "Token valid-token"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/user").header("Authorization", "Token valid-token"))
        .andExpect(status().isNotFound());
  }
}
