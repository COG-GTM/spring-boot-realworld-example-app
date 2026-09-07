package io.spring.api.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {
  private UserRepository userRepository;
  private JwtService jwtService;
  private JwtTokenFilter filter;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    userRepository = mock(UserRepository.class);
    jwtService = mock(JwtService.class);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_continue_without_authentication_when_header_is_missing() throws Exception {
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), is((Object) null));
    verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_authenticate_user_from_token_header() throws Exception {
    User user = new User("email", "username", "password", "", "");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token abc");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(
        SecurityContextHolder.getContext().getAuthentication().getPrincipal(), is((Object) user));
  }

  @Test
  public void should_not_authenticate_when_user_is_missing() throws Exception {
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of("missing"));
    when(userRepository.findById("missing")).thenReturn(Optional.empty());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token abc");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication(), is((Object) null));
  }

  @Test
  public void should_ignore_malformed_header() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "abc");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    verify(jwtService, never()).getSubFromToken(org.mockito.ArgumentMatchers.anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication(), is((Object) null));
  }

  @Test
  public void should_not_authenticate_invalid_token() throws Exception {
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.empty());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token abc");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication(), is((Object) null));
  }
}
