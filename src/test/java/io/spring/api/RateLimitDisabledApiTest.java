package io.spring.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsersApi.class)
@Import({WebSecurityConfig.class, UserQueryService.class, JacksonCustomizations.class})
@TestPropertySource(properties = {"rate-limit.enabled=false", "rate-limit.auth.limit=1"})
public class RateLimitDisabledApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private UserService userService;

  @Test
  public void should_not_limit_when_disabled() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(
              post("/users/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"user\":{\"email\":\"john@jacob.com\",\"password\":\"wrong\"}}"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }
  }
}
