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
    user = new User("aisensiy@test.com", "aisensiy", "123", "bio", "image");
    userRepository.save(user);
  }

  @Test
  public void should_find_user_by_id() {
    Optional<UserData> userDataOptional = userQueryService.findById(user.getId());
    Assertions.assertTrue(userDataOptional.isPresent());

    UserData userData = userDataOptional.get();
    Assertions.assertEquals(user.getId(), userData.getId());
    Assertions.assertEquals(user.getEmail(), userData.getEmail());
    Assertions.assertEquals(user.getUsername(), userData.getUsername());
    Assertions.assertEquals(user.getBio(), userData.getBio());
    Assertions.assertEquals(user.getImage(), userData.getImage());
  }

  @Test
  public void should_return_empty_for_unknown_id() {
    Optional<UserData> userDataOptional = userQueryService.findById("does-not-exist");
    Assertions.assertFalse(userDataOptional.isPresent());
  }
}
