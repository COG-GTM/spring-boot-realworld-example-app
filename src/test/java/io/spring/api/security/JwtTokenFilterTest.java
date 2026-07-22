package io.spring.api.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_with_valid_token() throws Exception {
    User user = new User("email@test.com", "username", "pass", "", "");
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken(eq("valid-token"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    UsernamePasswordAuthenticationToken authentication =
        (UsernamePasswordAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication, is(notNullValue()));
    assertThat(authentication.getPrincipal(), instanceOf(User.class));
    assertThat(((User) authentication.getPrincipal()).getId(), is(user.getId()));
    assertThat(authentication.getDetails(), is(notNullValue()));
    assertThat(filterChain.getRequest(), is(notNullValue()));
  }

  @Test
  public void should_not_authenticate_when_header_absent() throws Exception {
    filter.doFilter(request, response, filterChain);
    assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "Token");
    filter.doFilter(request, response, filterChain);
    assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
  }

  @Test
  public void should_not_authenticate_when_token_invalid() throws Exception {
    request.addHeader("Authorization", "Token invalid");
    when(jwtService.getSubFromToken(eq("invalid"))).thenReturn(Optional.empty());
    filter.doFilter(request, response, filterChain);
    assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
  }

  @Test
  public void should_not_authenticate_when_user_not_found() throws Exception {
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken(eq("valid-token"))).thenReturn(Optional.of("missing-id"));
    when(userRepository.findById(eq("missing-id"))).thenReturn(Optional.empty());
    filter.doFilter(request, response, filterChain);
    assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
  }

  @Test
  public void should_continue_filter_chain_when_authenticated() throws Exception {
    User user = new User("email@test.com", "username", "pass", "", "");
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken(eq("valid-token"))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    verify(jwtService).getSubFromToken(eq("valid-token"));
    verify(userRepository).findById(eq(user.getId()));
    assertThat(filterChain.getResponse(), is(notNullValue()));
  }
}
