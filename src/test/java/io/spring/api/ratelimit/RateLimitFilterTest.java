package io.spring.api.ratelimit;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.api.HealthApi;
import io.spring.api.TagsApi;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({TagsApi.class, HealthApi.class})
@Import({RateLimitConfig.class, RateLimitFilter.class, WebSecurityConfig.class})
@TestPropertySource(
    properties = {"ratelimit.unauthenticated-per-minute=3", "ratelimit.authenticated-per-minute=5"})
public class RateLimitFilterTest {
  @Autowired private MockMvc mvc;

  @MockBean private TagsQueryService tagsQueryService;
  @MockBean private JwtService jwtService;
  @MockBean private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());
    when(jwtService.getSubFromToken(eq("abc"))).thenReturn(Optional.of("user-1"));
  }

  @Test
  public void unauthenticated_requests_are_limited_per_ip() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(get("/tags").header("X-Forwarded-For", "10.0.0.1")).andExpect(status().isOk());
    }
    MvcResult result =
        mvc.perform(get("/tags").header("X-Forwarded-For", "10.0.0.1"))
            .andExpect(status().is(429))
            .andExpect(header().string("Retry-After", matchesPattern("\\d+")))
            .andExpect(jsonPath("$.error").value("rate_limited"))
            .andReturn();
    String retryAfter = result.getResponse().getHeader("Retry-After");
    org.junit.jupiter.api.Assertions.assertEquals(
        Integer.parseInt(retryAfter),
        (int)
            com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.retry_after_seconds"));
  }

  @Test
  public void authenticated_requests_use_user_key_and_higher_limit() throws Exception {
    for (int i = 0; i < 5; i++) {
      mvc.perform(get("/tags").header("Authorization", "Token abc")).andExpect(status().isOk());
    }
    mvc.perform(get("/tags").header("Authorization", "Token abc"))
        .andExpect(status().is(429))
        .andExpect(header().string("Retry-After", matchesPattern("\\d+")))
        .andExpect(jsonPath("$.error").value("rate_limited"));

    // a different IP bucket is unaffected by the authenticated user's exhaustion
    mvc.perform(get("/tags").header("X-Forwarded-For", "10.0.0.2")).andExpect(status().isOk());
  }

  @Test
  public void health_endpoint_is_excluded_from_rate_limiting() throws Exception {
    for (int i = 0; i < 10; i++) {
      mvc.perform(get("/health").header("X-Forwarded-For", "10.0.0.3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ok"));
    }
  }
}
