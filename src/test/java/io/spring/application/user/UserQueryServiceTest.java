package io.spring.application.user;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import java.util.UUID;
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
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "bio", "default_avatar.jpg");
    userRepository.save(user);
  }

  @Test
  public void should_fetch_user_by_id_success() {
    Optional<UserData> optional = userQueryService.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());

    UserData userData = optional.get();
    Assertions.assertEquals(user.getId(), userData.getId());
    Assertions.assertEquals("aisensiy@gmail.com", userData.getEmail());
    Assertions.assertEquals("aisensiy", userData.getUsername());
    Assertions.assertEquals("bio", userData.getBio());
    Assertions.assertEquals("default_avatar.jpg", userData.getImage());
  }

  @Test
  public void should_return_empty_for_unknown_id() {
    Assertions.assertFalse(userQueryService.findById(UUID.randomUUID().toString()).isPresent());
  }

  @Test
  public void should_return_empty_for_null_or_empty_id() {
    Assertions.assertFalse(userQueryService.findById(null).isPresent());
    Assertions.assertFalse(userQueryService.findById("").isPresent());
  }

  @Test
  public void should_fetch_the_right_user_when_several_users_exist() {
    User anotherUser = new User("other@test.com", "other", "456", "other bio", "other.jpg");
    userRepository.save(anotherUser);

    UserData userData = userQueryService.findById(anotherUser.getId()).get();
    Assertions.assertEquals(anotherUser.getId(), userData.getId());
    Assertions.assertEquals("other", userData.getUsername());
    Assertions.assertEquals("other bio", userData.getBio());
    Assertions.assertEquals("other.jpg", userData.getImage());
  }

  @Test
  public void should_fetch_user_with_empty_bio_and_image() {
    User emptyProfileUser = new User("empty@test.com", "empty", "123", "", "");
    userRepository.save(emptyProfileUser);

    UserData userData = userQueryService.findById(emptyProfileUser.getId()).get();
    Assertions.assertEquals("", userData.getBio());
    Assertions.assertEquals("", userData.getImage());
  }

  @Test
  public void should_fetch_updated_user_fields() {
    user.update("new@test.com", "newname", "", "new bio", "new.jpg");
    userRepository.save(user);

    UserData userData = userQueryService.findById(user.getId()).get();
    Assertions.assertEquals("new@test.com", userData.getEmail());
    Assertions.assertEquals("newname", userData.getUsername());
    Assertions.assertEquals("new bio", userData.getBio());
    Assertions.assertEquals("new.jpg", userData.getImage());
  }
}
