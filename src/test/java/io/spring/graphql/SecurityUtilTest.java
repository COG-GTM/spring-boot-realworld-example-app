package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class SecurityUtilTest {

  @Mock private SecurityContext securityContext;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "testuser", "password", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUser_withAuthenticatedUser_returnsUser() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals(user, result.get());
  }

  @Test
  void getCurrentUser_withAnonymousAuthentication_returnsEmpty() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    when(securityContext.getAuthentication()).thenReturn(anonymousToken);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertFalse(result.isPresent());
  }

  @Test
  void getCurrentUser_withNullPrincipal_returnsEmpty() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertFalse(result.isPresent());
  }

  @Test
  void getCurrentUser_withNullAuthentication_throwsNullPointerException() {
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(NullPointerException.class, () -> SecurityUtil.getCurrentUser());
  }

  @Test
  void getCurrentUser_returnsCorrectUserDetails() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals("test@test.com", result.get().getEmail());
    assertEquals("testuser", result.get().getUsername());
    assertEquals("bio", result.get().getBio());
    assertEquals("image", result.get().getImage());
  }

  @Test
  void getCurrentUser_withDifferentUser_returnsCorrectUser() {
    User anotherUser = new User("other@test.com", "otheruser", "password", "other bio", "other image");
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(anotherUser, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals("other@test.com", result.get().getEmail());
    assertEquals("otheruser", result.get().getUsername());
  }

  @Test
  void getCurrentUser_multipleCallsReturnSameUser() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result1 = SecurityUtil.getCurrentUser();
    Optional<User> result2 = SecurityUtil.getCurrentUser();

    assertTrue(result1.isPresent());
    assertTrue(result2.isPresent());
    assertEquals(result1.get(), result2.get());
  }

  @Test
  void getCurrentUser_withAuthenticationHavingCredentials_returnsUser() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            user, "credentials", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<User> result = SecurityUtil.getCurrentUser();

    assertTrue(result.isPresent());
    assertEquals(user, result.get());
  }
}
