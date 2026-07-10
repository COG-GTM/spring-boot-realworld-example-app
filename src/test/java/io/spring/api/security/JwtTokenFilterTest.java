package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserRepository userRepository;

  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain chain;
  private AutoCloseable closeable;

  private User user;
  private final String token = "valid.jwt.token";

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();

    user = new User("email@example.com", "username", "123", "", "");
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() throws Exception {
    SecurityContextHolder.clearContext();
    closeable.close();
  }

  private void doFilter() {
    ReflectionTestUtils.invokeMethod(filter, "doFilterInternal", request, response, chain);
  }

  private Authentication currentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  @Test
  public void should_populate_security_context_with_valid_token() {
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    doFilter();

    Authentication authentication = currentAuthentication();
    assertNotNull(authentication);
    assertSame(user, authentication.getPrincipal());
    assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
    assertTrue(authentication.getAuthorities().isEmpty());
    assertTrue(authentication.getDetails() instanceof WebAuthenticationDetails);
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_header_missing() {
    doFilter();

    assertNull(currentAuthentication());
    verifyNoInteractions(jwtService);
    verifyNoInteractions(userRepository);
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() {
    request.addHeader("Authorization", "Token");

    doFilter();

    assertNull(currentAuthentication());
    verifyNoInteractions(jwtService);
    verifyNoInteractions(userRepository);
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_token_is_invalid() {
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.empty());

    doFilter();

    assertNull(currentAuthentication());
    verifyNoInteractions(userRepository);
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_authenticate_when_user_not_found() {
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.empty());

    doFilter();

    assertNull(currentAuthentication());
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_not_overwrite_existing_authentication() {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("existing-principal", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));

    doFilter();

    assertSame(existing, currentAuthentication());
    verify(userRepository, never()).findById(eq(user.getId()));
    assertNotNull(chain.getRequest());
  }

  @Test
  public void should_extract_token_by_position_regardless_of_scheme() {
    request.addHeader("Authorization", "Bearer " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    doFilter();

    Authentication authentication = currentAuthentication();
    assertNotNull(authentication);
    assertSame(user, authentication.getPrincipal());
  }

  @Test
  public void should_always_continue_filter_chain() {
    request.addHeader("Authorization", "Token " + token);
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.empty());

    doFilter();

    assertSame(request, chain.getRequest());
    assertSame(response, chain.getResponse());
  }
}
