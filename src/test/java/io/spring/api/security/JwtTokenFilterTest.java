package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  private JwtTokenFilter filter;
  private UserRepository userRepository;
  private JwtService jwtService;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new JwtTokenFilter();
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_set_authentication_when_valid_token() throws ServletException, IOException {
    User user = new User("a@b.com", "testuser", "pass", "", "");
    when(request.getHeader("Authorization")).thenReturn("Token validtoken");
    when(jwtService.getSubFromToken("validtoken")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_no_header() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_invalid_header_format()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_invalid_token()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Token invalidtoken");
    when(jwtService.getSubFromToken("invalidtoken")).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_user_not_found()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Token validtoken");
    when(jwtService.getSubFromToken("validtoken")).thenReturn(Optional.of("unknown-id"));
    when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_override_existing_authentication() throws ServletException, IOException {
    User user = new User("a@b.com", "testuser", "pass", "", "");
    // Set existing authentication
    org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user, null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    when(request.getHeader("Authorization")).thenReturn("Token validtoken");
    when(jwtService.getSubFromToken("validtoken")).thenReturn(Optional.of(user.getId()));

    filter.doFilterInternal(request, response, filterChain);

    // Should still be the original authentication
    assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
