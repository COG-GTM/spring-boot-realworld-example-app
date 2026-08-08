package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
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
    verify(jwtService, never()).getSubFromToken(anyString());
  }

  @Test
  public void should_not_authenticate_with_malformed_header() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(jwtService, never()).getSubFromToken(anyString());
  }

  @Test
  public void should_accept_token_regardless_of_scheme_prefix() throws Exception {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    request.addHeader("Authorization", "Bearer valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isSameAs(user);
  }

  @Test
  public void should_not_authenticate_header_with_double_space() throws Exception {
    request.addHeader("Authorization", "Token  valid-token");
    when(jwtService.getSubFromToken(anyString())).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(userRepository, never()).findById(anyString());
  }

  @Test
  public void should_not_authenticate_with_invalid_token() throws Exception {
    request.addHeader("Authorization", "Token invalid-token");
    when(jwtService.getSubFromToken("invalid-token")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(userRepository, never()).findById(anyString());
  }

  @Test
  public void should_not_authenticate_when_user_is_missing() throws Exception {
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of("123"));
    when(userRepository.findById("123")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_set_authentication_for_valid_token() throws Exception {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.getAuthorities()).isEmpty();
    assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_not_overwrite_existing_authentication() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("existing", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of("123"));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(userRepository, never()).findById(anyString());
  }
}
