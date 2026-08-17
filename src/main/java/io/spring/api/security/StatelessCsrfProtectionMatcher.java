package io.spring.api.security;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Applies CSRF protection to the state-changing requests that can actually be forged by a browser:
 * requests that authenticate through an ambient credential, i.e. a session cookie.
 *
 * <p>Requests carrying a JWT in the {@code Authorization} header are exempt because a browser never
 * attaches that header to a cross-site request, and so are requests without any session cookie,
 * which have no credential to abuse and are rejected by the authorization rules instead.
 */
public class StatelessCsrfProtectionMatcher implements RequestMatcher {

  private static final Set<String> SAFE_METHODS =
      new HashSet<>(asList("GET", "HEAD", "TRACE", "OPTIONS"));

  private static final String SESSION_COOKIE = "JSESSIONID";

  @Override
  public boolean matches(HttpServletRequest request) {
    if (SAFE_METHODS.contains(request.getMethod())) {
      return false;
    }
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
      return false;
    }
    return hasSessionCookie(request);
  }

  private boolean hasSessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return false;
    }
    for (Cookie cookie : cookies) {
      if (SESSION_COOKIE.equalsIgnoreCase(cookie.getName())) {
        return true;
      }
    }
    return false;
  }
}
