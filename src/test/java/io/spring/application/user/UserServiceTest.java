package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://default.img/avatar.png";

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  void should_create_user_with_encoded_password_default_image_and_save() {
    when(passwordEncoder.encode("plain")).thenReturn("encoded");
    RegisterParam param = new RegisterParam("e@test.com", "newuser", "plain");

    User user = userService.createUser(param);

    assertEquals("e@test.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("encoded", user.getPassword());
    assertEquals(DEFAULT_IMAGE, user.getImage());
    assertEquals("", user.getBio());
    assertNotNull(user.getId());

    verify(passwordEncoder).encode("plain");
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(user, captor.getValue());
  }

  @Test
  void should_update_all_fields_when_all_present() {
    User target = new User("old@test.com", "old", "oldpass", "oldbio", "oldimg");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@test.com")
            .username("new")
            .password("newpass")
            .bio("newbio")
            .image("newimg")
            .build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("new@test.com", target.getEmail());
    assertEquals("new", target.getUsername());
    assertEquals("newpass", target.getPassword());
    assertEquals("newbio", target.getBio());
    assertEquals("newimg", target.getImage());
    verify(userRepository).save(target);
  }

  @Test
  void should_only_update_non_empty_fields_on_partial_update() {
    User target = new User("old@test.com", "old", "oldpass", "oldbio", "oldimg");
    UpdateUserParam param = UpdateUserParam.builder().email("new@test.com").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("new@test.com", target.getEmail());
    assertEquals("old", target.getUsername());
    assertEquals("oldpass", target.getPassword());
    assertEquals("oldbio", target.getBio());
    assertEquals("oldimg", target.getImage());
    verify(userRepository).save(target);
  }

  @Test
  void should_leave_user_unchanged_when_update_param_is_empty() {
    User target = new User("old@test.com", "old", "oldpass", "oldbio", "oldimg");
    UpdateUserParam param = UpdateUserParam.builder().build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("old@test.com", target.getEmail());
    assertEquals("old", target.getUsername());
    assertEquals("oldpass", target.getPassword());
    assertEquals("oldbio", target.getBio());
    assertEquals("oldimg", target.getImage());
    verify(userRepository).save(target);
  }
}
