package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  private JwtTokenFilter jwtTokenFilter;

  @Mock private UserRepository userRepository;

  @Mock private JwtService jwtService;

  @Mock private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    jwtTokenFilter = new JwtTokenFilter();
    ReflectionTestUtils.setField(jwtTokenFilter, "userRepository", userRepository);
    ReflectionTestUtils.setField(jwtTokenFilter, "jwtService", jwtService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_pass_without_setting_auth_when_no_authorization_header() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_set_authentication_when_valid_token() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token validtoken");
    MockHttpServletResponse response = new MockHttpServletResponse();

    User user = new User("e@t.com", "user", "pass", "", "");
    when(jwtService.getSubFromToken("validtoken")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_not_set_auth_when_token_is_invalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Token invalidtoken");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jwtService.getSubFromToken("invalidtoken")).thenReturn(Optional.empty());

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void should_not_set_auth_when_header_has_single_segment() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "BearerOnly");
    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtTokenFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
