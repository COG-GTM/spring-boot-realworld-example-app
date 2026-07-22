package io.spring.application.user;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({UserQueryService.class, MyBatisUserRepository.class})
public class UserQueryServiceTest extends DbTestBase {

  @Autowired private UserQueryService userQueryService;
  @Autowired private UserRepository userRepository;

  @Test
  public void should_fetch_user_by_id_success() {
    User user = new User("user@test.com", "username", "123", "bio", "image");
    userRepository.save(user);

    Optional<UserData> optional = userQueryService.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());
    UserData userData = optional.get();
    Assertions.assertEquals(user.getId(), userData.getId());
    Assertions.assertEquals("user@test.com", userData.getEmail());
    Assertions.assertEquals("username", userData.getUsername());
  }

  @Test
  public void should_return_empty_when_user_not_found() {
    Optional<UserData> optional = userQueryService.findById("not-exist");
    Assertions.assertFalse(optional.isPresent());
  }
}
