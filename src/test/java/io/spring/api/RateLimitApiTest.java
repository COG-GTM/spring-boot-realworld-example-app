package io.spring.api;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.user.UserService;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest({UsersApi.class, ArticlesApi.class})
@Import({WebSecurityConfig.class, UserQueryService.class, JacksonCustomizations.class})
@TestPropertySource(
    properties = {
      "rate-limit.auth.limit=2",
      "rate-limit.auth.window=60s",
      "rate-limit.article-creation.limit=1",
      "rate-limit.article-creation.window=1h"
    })
public class RateLimitApiTest extends TestWithCurrentUser {
  private static final String LOGIN_BODY =
      "{\"user\":{\"email\":\"john@jacob.com\",\"password\":\"wrong\"}}";
  private static final String REGISTER_BODY =
      "{\"user\":{\"email\":\"\",\"username\":\"\",\"password\":\"\"}}";
  private static final String ARTICLE_BODY =
      "{\"article\":{\"title\":\"t\",\"description\":\"d\",\"body\":\"\",\"tagList\":[]}}";

  @Autowired private MockMvc mvc;

  @MockBean private UserService userService;
  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_return_429_with_contract_after_auth_limit_exceeded() throws Exception {
    String ip = "10.0.0.1";
    mvc.perform(login(ip))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("X-RateLimit-Limit", "2"))
        .andExpect(header().string("X-RateLimit-Remaining", "1"))
        .andExpect(header().string("X-RateLimit-Reset", secondsWithin(60)));
    mvc.perform(login(ip))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("X-RateLimit-Remaining", "0"));

    mvc.perform(login(ip))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        .andExpect(header().string("Retry-After", secondsWithin(60)))
        .andExpect(header().string("X-RateLimit-Limit", "2"))
        .andExpect(header().string("X-RateLimit-Remaining", "0"))
        .andExpect(header().string("X-RateLimit-Reset", secondsWithin(60)))
        .andExpect(jsonPath("$.message").value(startsWith("too many requests, retry after ")))
        .andExpect(
            jsonPath("$.retryAfter").value(allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(60))));
  }

  @Test
  public void should_share_auth_limit_between_register_and_login() throws Exception {
    String ip = "10.0.0.2";
    mvc.perform(register(ip)).andExpect(status().isUnprocessableEntity());
    mvc.perform(login(ip)).andExpect(status().isUnprocessableEntity());

    mvc.perform(register(ip)).andExpect(status().isTooManyRequests());
    mvc.perform(login(ip)).andExpect(status().isTooManyRequests());
  }

  @Test
  public void should_limit_auth_per_client_ip() throws Exception {
    mvc.perform(login("10.0.0.3"));
    mvc.perform(login("10.0.0.3"));
    mvc.perform(login("10.0.0.3")).andExpect(status().isTooManyRequests());

    mvc.perform(login("10.0.0.4"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("X-RateLimit-Remaining", "1"));
  }

  @Test
  public void should_return_429_after_article_creation_limit_exceeded() throws Exception {
    mvc.perform(createArticle("10.0.1.1"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("X-RateLimit-Limit", "1"))
        .andExpect(header().string("X-RateLimit-Remaining", "0"));

    mvc.perform(createArticle("10.0.1.2"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", secondsWithin(3600)))
        .andExpect(jsonPath("$.message").value(startsWith("too many requests, retry after ")));
  }

  @Test
  public void should_not_count_unauthenticated_article_creation() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(
              post("/articles")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(ARTICLE_BODY)
                  .with(remoteAddr("10.0.1.3")))
          .andExpect(status().isUnauthorized());
    }

    mvc.perform(createArticle("10.0.1.3")).andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void should_not_rate_limit_unannotated_endpoints() throws Exception {
    for (int i = 0; i < 5; i++) {
      mvc.perform(get("/articles").with(remoteAddr("10.0.2.1")))
          .andExpect(status().isOk())
          .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }
  }

  private MockHttpServletRequestBuilder login(String ip) {
    return post("/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(LOGIN_BODY)
        .with(remoteAddr(ip));
  }

  private MockHttpServletRequestBuilder register(String ip) {
    return post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(REGISTER_BODY)
        .with(remoteAddr(ip));
  }

  private MockHttpServletRequestBuilder createArticle(String ip) {
    return post("/articles")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Token " + token)
        .content(ARTICLE_BODY)
        .with(remoteAddr(ip));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(
      String ip) {
    return request -> {
      request.setRemoteAddr(ip);
      return request;
    };
  }

  private static Matcher<String> secondsWithin(long max) {
    return new org.hamcrest.TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String value) {
        try {
          long seconds = Long.parseLong(value);
          return seconds >= 1 && seconds <= max;
        } catch (NumberFormatException e) {
          return false;
        }
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendText("integer seconds between 1 and " + max);
      }
    };
  }
}
