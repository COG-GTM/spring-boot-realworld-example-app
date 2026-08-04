package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({UserQueryService.class, MyBatisUserRepository.class})
public class UserQueryServiceTest extends DbTestBase {
  @Autowired private UserQueryService userQueryService;
  @Autowired private UserRepository userRepository;

  @Test
  public void should_fetch_user_by_id() {
    User user = new User("john@test.com", "john", "123", "my bio", "my-image.png");
    userRepository.save(user);

    Optional<UserData> optional = userQueryService.findById(user.getId());

    assertTrue(optional.isPresent());
    UserData userData = optional.get();
    assertThat(userData.getId(), is(user.getId()));
    assertThat(userData.getEmail(), is("john@test.com"));
    assertThat(userData.getUsername(), is("john"));
    assertThat(userData.getBio(), is("my bio"));
    assertThat(userData.getImage(), is("my-image.png"));
  }

  @Test
  public void should_return_empty_optional_when_user_not_found() {
    Optional<UserData> optional = userQueryService.findById(UUID.randomUUID().toString());

    assertFalse(optional.isPresent());
  }
}
