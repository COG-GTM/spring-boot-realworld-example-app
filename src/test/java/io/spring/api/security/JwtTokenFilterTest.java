package io.spring.api.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.service.DefaultJwtService;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtTokenFilterTest {
  private static final String SECRET =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  private JwtTokenFilter filter;
  private DefaultJwtService jwtService;
  private User user;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    jwtService = new DefaultJwtService(SECRET, 3600);

    filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_authenticate_with_valid_token_scheme() throws Exception {
    Authentication auth = filterWithHeader("Token " + jwtService.toToken(user));
    Assertions.assertNotNull(auth);
    Assertions.assertSame(user, auth.getPrincipal());
  }

  @Test
  public void should_authenticate_with_bearer_scheme() throws Exception {
    Assertions.assertNotNull(filterWithHeader("Bearer " + jwtService.toToken(user)));
  }

  @Test
  public void should_not_authenticate_with_unknown_scheme() throws Exception {
    Assertions.assertNull(filterWithHeader("Basic " + jwtService.toToken(user)));
  }

  @Test
  public void should_not_authenticate_with_extra_header_segments() throws Exception {
    Assertions.assertNull(filterWithHeader("Token " + jwtService.toToken(user) + " extra"));
  }

  @Test
  public void should_not_authenticate_with_expired_token() throws Exception {
    String expired =
        Jwts.builder()
            .setSubject(user.getId())
            .setExpiration(new Date(System.currentTimeMillis() - 1000L))
            .signWith(key(), SignatureAlgorithm.HS512)
            .compact();
    Assertions.assertNull(filterWithHeader("Token " + expired));
  }

  @Test
  public void should_not_authenticate_with_token_missing_expiry() throws Exception {
    String noExpiry =
        Jwts.builder().setSubject(user.getId()).signWith(key(), SignatureAlgorithm.HS512).compact();
    Assertions.assertNull(filterWithHeader("Token " + noExpiry));
  }

  @Test
  public void should_not_authenticate_with_downgraded_algorithm() throws Exception {
    String hs256 =
        Jwts.builder()
            .setSubject(user.getId())
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
    Assertions.assertNull(filterWithHeader("Token " + hs256));
  }

  @Test
  public void should_parse_token_string_strictly() {
    Assertions.assertEquals(Optional.of("abc"), JwtTokenFilter.getTokenString("Token abc"));
    Assertions.assertEquals(Optional.of("abc"), JwtTokenFilter.getTokenString("bearer  abc "));
    Assertions.assertEquals(Optional.empty(), JwtTokenFilter.getTokenString(null));
    Assertions.assertEquals(Optional.empty(), JwtTokenFilter.getTokenString("abc"));
    Assertions.assertEquals(Optional.empty(), JwtTokenFilter.getTokenString("Basic abc"));
    Assertions.assertEquals(Optional.empty(), JwtTokenFilter.getTokenString("Token a b"));
  }

  private Authentication filterWithHeader(String header) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user");
    request.addHeader("Authorization", header);
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static SecretKeySpec key() {
    return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
  }
}
