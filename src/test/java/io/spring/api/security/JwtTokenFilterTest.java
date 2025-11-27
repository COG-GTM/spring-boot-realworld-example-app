package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;

  @Mock private JwtService jwtService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtTokenFilter jwtTokenFilter;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_user_with_valid_token() throws Exception {
    String token = "validToken";
    String userId = "user-id-123";
    User user = new User("test@example.com", "testuser", "password", "bio", "image");

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));
    when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
  }

  @Test
  public void should_not_authenticate_when_no_authorization_header() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).getSubFromToken(org.mockito.ArgumentMatchers.any());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_when_token_is_invalid() throws Exception {
    String invalidToken = "invalidToken";

    when(request.getHeader("Authorization")).thenReturn("Token " + invalidToken);
    when(jwtService.getSubFromToken(eq(invalidToken))).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_when_user_not_found() throws Exception {
    String token = "validToken";
    String userId = "non-existent-user-id";

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));
    when(userRepository.findById(eq(userId))).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_when_authorization_header_has_no_space() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("TokenWithoutSpace");

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).getSubFromToken(org.mockito.ArgumentMatchers.any());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_when_authorization_header_is_empty() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("");

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).getSubFromToken(org.mockito.ArgumentMatchers.any());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_extract_token_from_bearer_format() throws Exception {
    String token = "myToken";
    String userId = "user-id-123";
    User user = new User("test@example.com", "testuser", "password", "bio", "image");

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));
    when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService).getSubFromToken(eq(token));
  }

  @Test
  public void should_always_call_filter_chain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_override_existing_authentication() throws Exception {
    String token = "validToken";
    String userId = "user-id-123";
    User user = new User("test@example.com", "testuser", "password", "bio", "image");

    Authentication existingAuth = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    when(request.getHeader("Authorization")).thenReturn("Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_handle_authorization_header_with_multiple_spaces() throws Exception {
    String token = "myToken";
    String userId = "user-id-123";
    User user = new User("test@example.com", "testuser", "password", "bio", "image");

    when(request.getHeader("Authorization")).thenReturn("Token " + token + " extra");
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(userId));
    when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService).getSubFromToken(eq(token));
  }
}
