package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtTokenFilter filter;

  @BeforeEach
  public void setUp() {
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_set_authentication_when_token_is_valid() throws Exception {
    User user = new User("a@b.com", "alice", "secret", "", "");
    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertSame(user, auth.getPrincipal());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_authorization_header_is_missing()
      throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_header_is_malformed() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("malformed");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_user_is_not_found() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of("missing-id"));
    when(userRepository.findById("missing-id")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_set_authentication_when_token_invalid() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token bad");
    when(jwtService.getSubFromToken("bad")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_keep_existing_authentication() throws Exception {
    User existingUser = new User("a@b.com", "alice", "secret", "", "");
    UsernamePasswordAuthenticationToken existing =
        new UsernamePasswordAuthenticationToken(
            existingUser, null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of("other-id"));

    filter.doFilter(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertSame(existing, auth);
    verify(userRepository, never()).findById(eq("other-id"));
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
