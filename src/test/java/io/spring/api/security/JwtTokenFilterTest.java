package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private FilterChain filterChain;

  private AutoCloseable mocks;
  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private User user;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @AfterEach
  public void tearDown() throws Exception {
    SecurityContextHolder.clearContext();
    mocks.close();
  }

  @Test
  public void should_not_authenticate_when_no_authorization_header() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(jwtService, userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(jwtService, userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_token_is_invalid() throws Exception {
    request.addHeader("Authorization", "Token expired.jwt.value");
    when(jwtService.getSubFromToken(eq("expired.jwt.value"))).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(userRepository);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_user_of_token_is_gone() throws Exception {
    request.addHeader("Authorization", "Token valid.jwt.value");
    when(jwtService.getSubFromToken(eq("valid.jwt.value"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_authenticate_current_user_with_valid_token() throws Exception {
    request.addHeader("Authorization", "Token valid.jwt.value");
    when(jwtService.getSubFromToken(eq("valid.jwt.value"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.getAuthorities()).isEmpty();
    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_keep_existing_authentication_untouched() throws Exception {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    UsernamePasswordAuthenticationToken existing =
        new UsernamePasswordAuthenticationToken(anotherUser, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    request.addHeader("Authorization", "Token valid.jwt.value");
    when(jwtService.getSubFromToken(eq("valid.jwt.value"))).thenReturn(Optional.of(user.getId()));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    verify(userRepository, never()).findById(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_accept_any_header_prefix_before_the_token() throws Exception {
    // The filter only splits on whitespace, so the documented "Token " prefix is not actually
    // enforced: a "Bearer " prefixed header authenticates just the same.
    request.addHeader("Authorization", "Bearer valid.jwt.value");
    when(jwtService.getSubFromToken(eq("valid.jwt.value"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isSameAs(user);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_always_continue_the_filter_chain() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    MockHttpServletRequest withToken = new MockHttpServletRequest();
    withToken.addHeader("Authorization", "Token valid.jwt.value");
    when(jwtService.getSubFromToken(eq("valid.jwt.value"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    filter.doFilterInternal(withToken, response, filterChain);

    verify(filterChain, times(2)).doFilter(any(), eq(response));
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
