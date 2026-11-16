package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {
  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private JwtTokenFilter filter;
  private User user;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;

  @BeforeEach
  public void setUp() {
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_not_authenticate_when_header_missing() throws Exception {
    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(jwtService, userRepository);
    assertNotNull(chain.getRequest(), "filter chain must continue");
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() throws Exception {
    request.addHeader("Authorization", "Token");

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(jwtService, userRepository);
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_token_invalid() throws Exception {
    request.addHeader("Authorization", "Token bad-token");
    when(jwtService.getSubFromToken("bad-token")).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(anyString());
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_token_subject_has_no_user() throws Exception {
    request.addHeader("Authorization", "Token orphan-token");
    when(jwtService.getSubFromToken("orphan-token")).thenReturn(Optional.of("missing-id"));
    when(userRepository.findById("missing-id")).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_authenticate_with_valid_token() throws Exception {
    request.addHeader("Authorization", "Token good-token");
    when(jwtService.getSubFromToken("good-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertSame(user, authentication.getPrincipal());
    assertNull(authentication.getCredentials());
    assertNotNull(authentication.getDetails());
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_accept_bearer_prefix_as_well() throws Exception {
    request.addHeader("Authorization", "Bearer good-token");
    when(jwtService.getSubFromToken("good-token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    filter.doFilter(request, response, chain);

    assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
  }

  @Test
  public void should_not_override_existing_authentication() throws Exception {
    User other = new User("other@example.com", "other", "123", "", "");
    UsernamePasswordAuthenticationToken existing =
        new UsernamePasswordAuthenticationToken(other, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);
    request.addHeader("Authorization", "Token good-token");
    when(jwtService.getSubFromToken("good-token")).thenReturn(Optional.of(user.getId()));

    filter.doFilter(request, response, chain);

    assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(anyString());
  }
}
