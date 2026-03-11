package io.spring.application.user;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({UserQueryService.class, MyBatisUserRepository.class})
public class UserQueryServiceTest extends DbTestBase {
  @Autowired private UserQueryService userQueryService;
  @Autowired private UserRepository userRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("test@test.com", "testuser", "password123", "test bio", "http://image.url");
    userRepository.save(user);
  }

  @Test
  public void should_find_user_by_id_successfully() {
    Optional<UserData> optional = userQueryService.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());

    UserData userData = optional.get();
    Assertions.assertEquals(user.getId(), userData.getId());
    Assertions.assertEquals(user.getEmail(), userData.getEmail());
    Assertions.assertEquals(user.getUsername(), userData.getUsername());
    Assertions.assertEquals(user.getBio(), userData.getBio());
    Assertions.assertEquals(user.getImage(), userData.getImage());
  }

  @Test
  public void should_return_empty_when_user_not_found() {
    Optional<UserData> optional = userQueryService.findById("non-existent-id");
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_find_user_with_empty_bio_and_image() {
    User userWithEmptyFields = new User("empty@test.com", "emptyuser", "password", "", "");
    userRepository.save(userWithEmptyFields);

    Optional<UserData> optional = userQueryService.findById(userWithEmptyFields.getId());
    Assertions.assertTrue(optional.isPresent());

    UserData userData = optional.get();
    Assertions.assertEquals("", userData.getBio());
    Assertions.assertEquals("", userData.getImage());
  }

  @Test
  public void should_find_multiple_users_independently() {
    User user2 = new User("user2@test.com", "user2", "password", "bio2", "image2");
    userRepository.save(user2);

    Optional<UserData> optional1 = userQueryService.findById(user.getId());
    Optional<UserData> optional2 = userQueryService.findById(user2.getId());

    Assertions.assertTrue(optional1.isPresent());
    Assertions.assertTrue(optional2.isPresent());
    Assertions.assertNotEquals(optional1.get().getId(), optional2.get().getId());
    Assertions.assertEquals("testuser", optional1.get().getUsername());
    Assertions.assertEquals("user2", optional2.get().getUsername());
  }
}
