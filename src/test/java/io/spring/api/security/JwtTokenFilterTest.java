package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserRepository userRepository;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  @InjectMocks private JwtTokenFilter jwtTokenFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_continue_filter_chain_when_no_authorization_header() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void should_continue_filter_chain_when_malformed_header_no_space() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("TokenWithNoSpace");

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void should_set_authentication_when_valid_token_and_user_exists() throws Exception {
    String token = "valid-token";
    User user = new User("test@test.com", "testuser", "pass", "", "");

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
  }

  @Test
  void should_not_set_authentication_when_valid_token_but_user_not_in_db() throws Exception {
    String token = "valid-token";
    String userId = "non-existent-user-id";

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));
    when(userRepository.findById(eq(userId))).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void should_not_overwrite_existing_authentication() throws Exception {
    String token = "valid-token";
    User user = new User("test@test.com", "testuser", "pass", "", "");

    UsernamePasswordAuthenticationToken existingAuth =
        new UsernamePasswordAuthenticationToken("existing-principal", null);
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
  }
}
