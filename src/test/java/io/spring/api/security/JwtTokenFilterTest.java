package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private JwtTokenFilter filter;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;

  @BeforeEach
  public void setUp() {
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_user_from_valid_token() throws Exception {
    User user = new User("email@test.com", "username", "123", "", "");
    when(jwtService.getSubFromToken("token")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token token");

    filter.doFilter(request, response, filterChain);

    assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertEquals(request, filterChain.getRequest());
  }

  @Test
  public void should_not_authenticate_without_authorization_header() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_with_malformed_header() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "token");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  public void should_not_authenticate_with_invalid_token() throws Exception {
    when(jwtService.getSubFromToken("token")).thenReturn(Optional.empty());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token token");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService).getSubFromToken("token");
  }

  @Test
  public void should_not_authenticate_when_user_is_missing() throws Exception {
    when(jwtService.getSubFromToken("token")).thenReturn(Optional.of("123"));
    when(userRepository.findById("123")).thenReturn(Optional.empty());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token token");

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
