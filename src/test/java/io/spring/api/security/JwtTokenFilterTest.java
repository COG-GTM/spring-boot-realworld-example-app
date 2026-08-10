package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
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

class JwtTokenFilterTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final JwtService jwtService = mock(JwtService.class);
  private final JwtTokenFilter filter = new JwtTokenFilter();

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_not_authenticate_when_authorization_header_is_missing() throws Exception {
    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_not_authenticate_when_authorization_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(jwtService, never()).getSubFromToken(anyString());
  }

  @Test
  void should_not_authenticate_when_token_is_invalid() throws Exception {
    request.addHeader("Authorization", "Token invalid-token");
    when(jwtService.getSubFromToken("invalid-token")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_not_authenticate_when_user_of_token_does_not_exist() throws Exception {
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of("123"));
    when(userRepository.findById("123")).thenReturn(Optional.empty());

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  void should_authenticate_user_of_valid_token() throws Exception {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getAuthorities()).isEmpty();
    assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  void should_keep_existing_authentication_untouched() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("already-authenticated", null, null);
    SecurityContextHolder.getContext().setAuthentication(existing);
    request.addHeader("Authorization", "Token valid-token");
    when(jwtService.getSubFromToken("valid-token")).thenReturn(Optional.of("123"));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    verifyNoInteractions(userRepository);
    assertThat(filterChain.getRequest()).isSameAs(request);
  }
}
