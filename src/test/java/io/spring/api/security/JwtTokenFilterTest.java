package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
  private static final String TOKEN = "valid.jwt.token";

  @Mock private UserRepository userRepository;

  @Mock private JwtService jwtService;

  private JwtTokenFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;
  private User user;

  @BeforeEach
  public void setUp() {
    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = new MockFilterChain();

    user =
        new User(
            "john@jacob.com",
            "johnjacob",
            "123",
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_not_authenticate_without_authorization_header() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verifyNoInteractions(jwtService, userRepository);
  }

  @Test
  public void should_not_authenticate_with_malformed_authorization_header() throws Exception {
    request.addHeader("Authorization", TOKEN);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verify(jwtService, never()).getSubFromToken(anyString());
    verifyNoInteractions(userRepository);
  }

  @Test
  public void should_not_authenticate_with_invalid_token() throws Exception {
    request.addHeader("Authorization", "Token " + TOKEN);
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
    verifyNoInteractions(userRepository);
  }

  @Test
  public void should_authenticate_with_valid_token_of_existing_user() throws Exception {
    request.addHeader("Authorization", "Token " + TOKEN);
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    filter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.getAuthorities()).isEmpty();
    assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_not_authenticate_with_valid_token_of_unknown_user() throws Exception {
    request.addHeader("Authorization", "Token " + TOKEN);
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.of("unknown-user-id"));
    when(userRepository.findById(eq("unknown-user-id"))).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isSameAs(request);
  }

  @Test
  public void should_keep_existing_authentication_untouched() throws Exception {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("existing", null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existing);

    request.addHeader("Authorization", "Token " + TOKEN);
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.of(user.getId()));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    assertThat(filterChain.getRequest()).isSameAs(request);
    verifyNoInteractions(userRepository);
  }
}
