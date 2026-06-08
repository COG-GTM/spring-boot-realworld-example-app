package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder encryptService;

  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("e@t.com", "testuser", "encodedpass", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createUser_should_call_userService() {
    CreateUserInput input =
        CreateUserInput.newBuilder().email("e@t.com").username("testuser").password("pass").build();
    when(userService.createUser(any())).thenReturn(user);

    var result = userMutation.createUser(input);

    assertNotNull(result);
    verify(userService).createUser(any());
  }

  @Test
  void login_should_return_user_when_credentials_valid() {
    when(userRepository.findByEmail("e@t.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("pass", "encodedpass")).thenReturn(true);

    var result = userMutation.login("pass", "e@t.com");

    assertNotNull(result);
  }

  @Test
  void login_should_throw_when_credentials_invalid() {
    when(userRepository.findByEmail("e@t.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrongpass", "encodedpass")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrongpass", "e@t.com"));
  }

  @Test
  void updateUser_should_update_authenticated_user() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput input =
        UpdateUserInput.newBuilder().email("new@t.com").username("newuser").bio("newbio").build();

    var result = userMutation.updateUser(input);

    assertNotNull(result);
    verify(userService).updateUser(any());
  }

  @Test
  void updateUser_should_return_null_when_not_authenticated() {
    org.springframework.security.authentication.AnonymousAuthenticationToken anonAuth =
        new org.springframework.security.authentication.AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    UpdateUserInput input = UpdateUserInput.newBuilder().email("e@t.com").build();

    var result = userMutation.updateUser(input);
    assertNull(result);
  }
}
