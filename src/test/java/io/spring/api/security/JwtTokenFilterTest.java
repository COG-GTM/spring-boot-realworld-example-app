package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {
  @Mock private UserRepository userRepository;

  @Mock private JwtService jwtService;

  @Mock private FilterChain filterChain;

  private JwtTokenFilter filter;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_continue_without_authorization_header() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain)
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_authenticate_valid_token() throws Exception {
    User user = new User("email", "username", "password", "bio", "image");
    MockHttpServletRequest request = requestWithAuthorization("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(filterChain)
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_not_authenticate_invalid_token() throws Exception {
    when(jwtService.getSubFromToken("bad")).thenReturn(Optional.empty());

    filter.doFilter(
        requestWithAuthorization("Token bad"), new MockHttpServletResponse(), filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  public void should_ignore_header_without_token() throws Exception {
    filter.doFilter(requestWithAuthorization("Token"), new MockHttpServletResponse(), filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    org.mockito.Mockito.verifyNoInteractions(jwtService);
  }

  @Test
  public void should_not_authenticate_when_user_is_missing() throws Exception {
    User user = new User("email", "username", "password", "bio", "image");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

    filter.doFilter(
        requestWithAuthorization("Token abc"), new MockHttpServletResponse(), filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_overwrite_existing_authentication() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("existing", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);
    User user = new User("email", "username", "password", "bio", "image");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));

    filter.doFilter(
        requestWithAuthorization("Token abc"), new MockHttpServletResponse(), filterChain);

    assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
  }

  private static MockHttpServletRequest requestWithAuthorization(String authorization) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", authorization);
    return request;
  }
}
