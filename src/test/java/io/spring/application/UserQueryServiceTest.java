package io.spring.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
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

  @Autowired private UserReadService userReadService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "my bio", "my image");
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
    assertThat(userData.getBio()).isEqualTo("my bio");
    assertThat(userData.getImage()).isEqualTo("my image");
  }

  @Test
  public void should_return_empty_optional_for_unknown_id() {
    assertThat(userQueryService.findById(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  public void should_return_empty_optional_for_null_id() {
    assertThat(userQueryService.findById(null)).isEmpty();
  }

  @Test
  public void should_read_the_updated_user_after_it_is_saved_again() {
    user.update("updated@gmail.com", "updated", "", "updated bio", "updated image");
    userRepository.save(user);

    UserData userData = userQueryService.findById(user.getId()).get();
    assertThat(userData.getEmail()).isEqualTo("updated@gmail.com");
    assertThat(userData.getUsername()).isEqualTo("updated");
    assertThat(userData.getBio()).isEqualTo("updated bio");
    assertThat(userData.getImage()).isEqualTo("updated image");
  }

  /**
   * {@link UserQueryService} exposes no username lookup, so the read path used by the profile and
   * login flows is asserted straight against {@link UserReadService}.
   */
  @Test
  public void should_read_user_by_username() {
    UserData userData = userReadService.findByUsername("aisensiy");

    assertThat(userData).isNotNull();
    assertThat(userData.getId()).isEqualTo(user.getId());
    assertThat(userData.getEmail()).isEqualTo("aisensiy@gmail.com");
    assertThat(userData.getBio()).isEqualTo("my bio");
    assertThat(userData.getImage()).isEqualTo("my image");
  }

  @Test
  public void should_read_null_for_unknown_username() {
    assertThat(userReadService.findByUsername("nobody")).isNull();
  }
}
