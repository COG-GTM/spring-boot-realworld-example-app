package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class StatelessCsrfProtectionMatcherTest {

  private final StatelessCsrfProtectionMatcher matcher = new StatelessCsrfProtectionMatcher();

  @Test
  public void should_require_csrf_token_for_session_cookie_requests() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/articles");
    request.setCookies(new Cookie("JSESSIONID", "abc"));

    assertTrue(matcher.matches(request));
  }

  @Test
  public void should_not_require_csrf_token_for_bearer_token_requests() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/articles");
    request.setCookies(new Cookie("JSESSIONID", "abc"));
    request.addHeader("Authorization", "Token jwt");

    assertFalse(matcher.matches(request));
  }

  @Test
  public void should_not_require_csrf_token_without_ambient_credential() {
    assertFalse(matcher.matches(new MockHttpServletRequest("POST", "/users/login")));
  }

  @Test
  public void should_not_require_csrf_token_for_safe_methods() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    request.setCookies(new Cookie("JSESSIONID", "abc"));

    assertFalse(matcher.matches(request));
  }
}
