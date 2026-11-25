package io.spring.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;

  @Mock private JwtService jwtService;

  @InjectMocks private JwtTokenFilter filter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;
  private User user;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_user_from_valid_token() throws Exception {
    request.addHeader("Authorization", "Token abc");
    when(jwtService.getSubFromToken(eq("abc"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilter(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertNotNull(authentication);
    Assertions.assertSame(user, authentication.getPrincipal());
    Assertions.assertTrue(authentication.isAuthenticated());
    Assertions.assertNotNull(authentication.getDetails());
    Assertions.assertSame(request, chain.getRequest());
  }

  @Test
  public void should_skip_when_header_missing() throws Exception {
    filter.doFilter(request, response, chain);

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    Assertions.assertSame(request, chain.getRequest());
  }

  @Test
  public void should_skip_when_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "abc");

    filter.doFilter(request, response, chain);

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    Assertions.assertSame(request, chain.getRequest());
  }

  @Test
  public void should_skip_when_token_invalid() throws Exception {
    request.addHeader("Authorization", "Token invalid");
    when(jwtService.getSubFromToken(eq("invalid"))).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(any());
    Assertions.assertSame(request, chain.getRequest());
  }

  @Test
  public void should_skip_when_user_not_found() throws Exception {
    request.addHeader("Authorization", "Token abc");
    when(jwtService.getSubFromToken(eq("abc"))).thenReturn(Optional.of("missing"));
    when(userRepository.findById(eq("missing"))).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    Assertions.assertSame(request, chain.getRequest());
  }

  @Test
  public void should_not_override_existing_authentication() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("someone", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);
    request.addHeader("Authorization", "Token abc");
    when(jwtService.getSubFromToken(eq("abc"))).thenReturn(Optional.of(user.getId()));

    filter.doFilter(request, response, chain);

    Assertions.assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(any());
  }
}
