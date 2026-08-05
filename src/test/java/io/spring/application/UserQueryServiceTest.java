package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import java.util.UUID;
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
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "bio", "image");
    userRepository.save(user);
  }

  @Test
  public void should_fetch_user_by_id() {
    Optional<UserData> optional = userQueryService.findById(user.getId());

    assertThat(optional).isPresent();
    UserData userData = optional.get();
    assertThat(userData.getId()).isEqualTo(user.getId());
    assertThat(userData.getEmail()).isEqualTo("aisensiy@gmail.com");
    assertThat(userData.getUsername()).isEqualTo("aisensiy");
    assertThat(userData.getBio()).isEqualTo("bio");
    assertThat(userData.getImage()).isEqualTo("image");
  }

  @Test
  public void should_get_empty_optional_for_unknown_id() {
    assertThat(userQueryService.findById(UUID.randomUUID().toString())).isEmpty();
  }
}
