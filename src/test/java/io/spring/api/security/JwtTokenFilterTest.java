package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  private JwtTokenFilter jwtTokenFilter;
  private UserRepository userRepository;
  private JwtService jwtService;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    jwtTokenFilter = new JwtTokenFilter();
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    ReflectionTestUtils.setField(jwtTokenFilter, "userRepository", userRepository);
    ReflectionTestUtils.setField(jwtTokenFilter, "jwtService", jwtService);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_set_authentication_with_valid_token() throws Exception {
    User user = new User("test@test.com", "testuser", "password", "", "");
    when(request.getHeader("Authorization")).thenReturn("Token validtoken");
    when(jwtService.getSubFromToken(eq("validtoken"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_continue_chain_without_auth_header() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_continue_chain_with_invalid_token_format() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_continue_chain_when_token_invalid() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token badtoken");
    when(jwtService.getSubFromToken(eq("badtoken"))).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_continue_chain_when_user_not_found() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token validtoken");
    when(jwtService.getSubFromToken(eq("validtoken"))).thenReturn(Optional.of("unknown-id"));
    when(userRepository.findById(eq("unknown-id"))).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
