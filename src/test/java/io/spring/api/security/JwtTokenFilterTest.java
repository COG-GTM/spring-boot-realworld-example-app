package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  private JwtTokenFilter filter;
  private UserRepository userRepository;
  private JwtService jwtService;
  private FilterChain filterChain;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private User user;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    filterChain = mock(FilterChain.class);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_user_with_valid_token() throws Exception {
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertEquals(user, authentication.getPrincipal());
    assertNotNull(authentication.getDetails());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_continue_chain_without_authorization_header() throws Exception {
    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_ignore_header_without_token_part() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_with_invalid_token() throws Exception {
    request.addHeader("Authorization", "Token bad-token");
    when(jwtService.getSubFromToken("bad-token")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_user_not_found() throws Exception {
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of("missing-id"));
    when(userRepository.findById("missing-id")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_keep_existing_authentication() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken(
            "existing", null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of(user.getId()));

    filter.doFilter(request, response, filterChain);

    assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(any());
    verify(filterChain).doFilter(eq(request), eq(response));
  }
}
