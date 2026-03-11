package io.spring.api.security;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RateLimitingFilterTest {

  private RateLimitingFilter rateLimitingFilter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  public void setUp() {
    // Allow 5 requests per minute for easier testing
    rateLimitingFilter = new RateLimitingFilter(5, 5, 1);
    request = Mockito.mock(HttpServletRequest.class);
    response = Mockito.mock(HttpServletResponse.class);
    filterChain = Mockito.mock(FilterChain.class);
  }

  @Test
  public void should_allow_non_login_requests() throws Exception {
    Mockito.when(request.getMethod()).thenReturn("GET");
    Mockito.when(request.getRequestURI()).thenReturn("/articles");

    for (int i = 0; i < 20; i++) {
      rateLimitingFilter.doFilterInternal(request, response, filterChain);
    }

    Mockito.verify(filterChain, Mockito.times(20)).doFilter(request, response);
  }

  @Test
  public void should_allow_login_within_limit() throws Exception {
    Mockito.when(request.getMethod()).thenReturn("POST");
    Mockito.when(request.getRequestURI()).thenReturn("/users/login");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    for (int i = 0; i < 5; i++) {
      rateLimitingFilter.doFilterInternal(request, response, filterChain);
    }

    Mockito.verify(filterChain, Mockito.times(5)).doFilter(request, response);
  }

  @Test
  public void should_block_login_exceeding_limit() throws Exception {
    Mockito.when(request.getMethod()).thenReturn("POST");
    Mockito.when(request.getRequestURI()).thenReturn("/users/login");
    Mockito.when(request.getRemoteAddr()).thenReturn("192.168.1.1");

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    Mockito.when(response.getWriter()).thenReturn(printWriter);

    // Exhaust the bucket
    for (int i = 0; i < 5; i++) {
      rateLimitingFilter.doFilterInternal(request, response, filterChain);
    }

    // This one should be blocked
    rateLimitingFilter.doFilterInternal(request, response, filterChain);

    Mockito.verify(filterChain, Mockito.times(5)).doFilter(request, response);
    Mockito.verify(response).setStatus(429);
  }

  @Test
  public void should_track_different_ips_separately() throws Exception {
    HttpServletRequest request1 = Mockito.mock(HttpServletRequest.class);
    HttpServletRequest request2 = Mockito.mock(HttpServletRequest.class);

    Mockito.when(request1.getMethod()).thenReturn("POST");
    Mockito.when(request1.getRequestURI()).thenReturn("/users/login");
    Mockito.when(request1.getRemoteAddr()).thenReturn("10.0.0.1");

    Mockito.when(request2.getMethod()).thenReturn("POST");
    Mockito.when(request2.getRequestURI()).thenReturn("/users/login");
    Mockito.when(request2.getRemoteAddr()).thenReturn("10.0.0.2");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(pw);

    // Exhaust bucket for IP 1
    for (int i = 0; i < 5; i++) {
      rateLimitingFilter.doFilterInternal(request1, response, filterChain);
    }

    // IP 2 should still work
    rateLimitingFilter.doFilterInternal(request2, response, filterChain);

    Mockito.verify(filterChain, Mockito.times(5)).doFilter(request1, response);
    Mockito.verify(filterChain, Mockito.times(1)).doFilter(request2, response);
  }

  @Test
  public void should_use_x_forwarded_for_header() throws Exception {
    Mockito.when(request.getMethod()).thenReturn("POST");
    Mockito.when(request.getRequestURI()).thenReturn("/users/login");
    Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");
    Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(pw);

    // Exhaust limit
    for (int i = 0; i < 5; i++) {
      rateLimitingFilter.doFilterInternal(request, response, filterChain);
    }

    // 6th should be blocked
    rateLimitingFilter.doFilterInternal(request, response, filterChain);

    Mockito.verify(filterChain, Mockito.times(5)).doFilter(request, response);
    Mockito.verify(response).setStatus(429);
  }
}
