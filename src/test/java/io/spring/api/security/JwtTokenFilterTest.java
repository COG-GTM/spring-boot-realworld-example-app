package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  private UserRepository userRepository;
  private JwtService jwtService;
  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);

    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void doFilter() throws Exception {
    ReflectionTestUtils.invokeMethod(filter, "doFilterInternal", request, response, filterChain);
  }

  @Test
  public void should_authenticate_current_user_with_valid_token() throws Exception {
    request.addHeader("Authorization", "Token valid-jwt");
    when(jwtService.getSubFromToken(eq("valid-jwt"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    doFilter();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertSame(user, authentication.getPrincipal());
    assertNull(authentication.getCredentials());
    assertEquals(Collections.emptyList(), authentication.getAuthorities());
    assertTrue(authentication.getDetails() instanceof WebAuthenticationDetails);
    assertTrue(authentication.isAuthenticated());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_with_invalid_or_expired_token() throws Exception {
    request.addHeader("Authorization", "Token expired-jwt");
    when(jwtService.getSubFromToken(eq("expired-jwt"))).thenReturn(Optional.empty());

    doFilter();

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(userRepository);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_header_is_missing() throws Exception {
    doFilter();

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verifyNoInteractions(userRepository);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "Token");

    doFilter();

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_header_is_empty() throws Exception {
    request.addHeader("Authorization", "");

    doFilter();

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).getSubFromToken(anyString());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_user_not_found() throws Exception {
    request.addHeader("Authorization", "Token valid-jwt");
    when(jwtService.getSubFromToken(eq("valid-jwt"))).thenReturn(Optional.of("unknown-id"));
    when(userRepository.findById(eq("unknown-id"))).thenReturn(Optional.empty());

    doFilter();

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  public void should_keep_existing_authentication_untouched() throws Exception {
    UsernamePasswordAuthenticationToken existing =
        new UsernamePasswordAuthenticationToken("existing", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    request.addHeader("Authorization", "Token valid-jwt");
    when(jwtService.getSubFromToken(eq("valid-jwt"))).thenReturn(Optional.of(user.getId()));

    doFilter();

    assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(userRepository);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  /**
   * Characterization test, NOT a specification: {@code getTokenString} splits the header on
   * whitespace and takes index 1 without checking the scheme, so any prefix is accepted instead of
   * only the RealWorld {@code Token} scheme. If the scheme check is ever tightened, this test is
   * expected to fail and should be replaced by a strict assertion — it is not a regression.
   */
  @Test
  public void currently_authenticates_with_any_header_prefix_scheme_is_not_validated()
      throws Exception {
    request.addHeader("Authorization", "Bearer valid-jwt");
    when(jwtService.getSubFromToken(eq("valid-jwt"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    doFilter();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertSame(user, authentication.getPrincipal());
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
