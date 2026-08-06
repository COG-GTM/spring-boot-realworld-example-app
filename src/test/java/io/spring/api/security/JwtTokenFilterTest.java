package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  private JwtTokenFilter filter;
  private UserRepository userRepository;
  private JwtService jwtService;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;
  private User user;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    filter = new JwtTokenFilter();
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
    user = TestHelper.userFixture("jwt");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_not_authenticate_without_authorization_header() throws Exception {
    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(jwtService, never()).getSubFromToken(any());
  }

  @Test
  public void should_not_authenticate_when_header_has_no_space() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(jwtService, never()).getSubFromToken(any());
  }

  @Test
  public void should_authenticate_with_valid_token() throws Exception {
    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.getAuthorities()).isEmpty();
    assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_not_authenticate_when_token_is_invalid() throws Exception {
    String token = "invalid.jwt.token";
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(userRepository, never()).findById(any());
  }

  @Test
  public void should_not_authenticate_when_user_of_token_does_not_exist() throws Exception {
    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of("unknown-user-id"));
    when(userRepository.findById(eq("unknown-user-id"))).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_not_override_existing_authentication() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("already-here", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(userRepository, never()).findById(any());
  }
}
