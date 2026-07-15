package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserRepository userRepository;
  @InjectMocks private JwtTokenFilter jwtTokenFilter;

  @BeforeEach
  void clearSecurityContextBeforeTest() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clearSecurityContextAfterTest() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validTokenAuthenticatesResolvedUserAndContinuesFilterChain() throws Exception {
    User user = user();
    MockHttpServletRequest request = requestWithAuthorization("Token valid-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(jwtService.getSubFromToken("valid-jwt")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getDetails()).isNotNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void missingAuthorizationHeaderDoesNotAuthenticateAndContinuesFilterChain() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(jwtService, userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void malformedAuthorizationHeaderDoesNotAuthenticateAndContinuesFilterChain() throws Exception {
    MockHttpServletRequest request = requestWithAuthorization("Token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(jwtService, userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void nonStandardSchemeWithUnresolvableTokenDoesNotAuthenticateAndContinuesFilterChain()
      throws Exception {
    MockHttpServletRequest request = requestWithAuthorization("Bearer invalid-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(jwtService.getSubFromToken("invalid-jwt")).thenReturn(Optional.empty());

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jwtService).getSubFromToken("invalid-jwt");
    verifyNoInteractions(userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void authorizationHeaderWithExtraSpaceDoesNotAuthenticateAndContinuesFilterChain()
      throws Exception {
    MockHttpServletRequest request = requestWithAuthorization("Token  valid-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(jwtService.getSubFromToken("")).thenReturn(Optional.empty());

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jwtService).getSubFromToken("");
    verifyNoInteractions(userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void unresolvableTokenDoesNotAuthenticateAndContinuesFilterChain() throws Exception {
    MockHttpServletRequest request = requestWithAuthorization("Token invalid-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(jwtService.getSubFromToken("invalid-jwt")).thenReturn(Optional.empty());

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jwtService).getSubFromToken("invalid-jwt");
    verifyNoInteractions(userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void tokenForUnknownUserDoesNotAuthenticateAndContinuesFilterChain() throws Exception {
    User user = user();
    MockHttpServletRequest request = requestWithAuthorization("Token valid-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);
    when(jwtService.getSubFromToken("valid-jwt")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

    jwtTokenFilter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  private MockHttpServletRequest requestWithAuthorization(String value) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", value);
    return request;
  }

  private User user() {
    return new User("user@example.com", "user", "password", "", "");
  }
}
