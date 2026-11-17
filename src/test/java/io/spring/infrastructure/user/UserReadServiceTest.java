package io.spring.infrastructure.user;

import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(MyBatisUserRepository.class)
public class UserReadServiceTest extends DbTestBase {
  @Autowired private UserReadService userReadService;
  @Autowired private UserRepository userRepository;

  private User first;
  private User second;

  @BeforeEach
  public void setUp() {
    first = new User("first@test.com", "first", "123", "bio1", "img1");
    second = new User("second@test.com", "second", "123", "bio2", "img2");
    userRepository.save(first);
    userRepository.save(second);
    userRepository.save(new User("third@test.com", "third", "123", "", ""));
  }

  @Test
  public void should_find_only_requested_users_by_usernames() {
    List<UserData> users =
        userReadService.findByUsernames(Arrays.asList("first", "second", "missing"));

    Assertions.assertEquals(2, users.size());
    List<String> usernames =
        users.stream().map(UserData::getUsername).sorted().collect(Collectors.toList());
    Assertions.assertEquals(Arrays.asList("first", "second"), usernames);
    UserData fetched =
        users.stream().filter(u -> u.getUsername().equals("first")).findFirst().get();
    Assertions.assertEquals(first.getId(), fetched.getId());
    Assertions.assertEquals("bio1", fetched.getBio());
    Assertions.assertEquals("img1", fetched.getImage());
  }

  @Test
  public void should_return_empty_list_when_no_username_matches() {
    List<UserData> users = userReadService.findByUsernames(Collections.singletonList("nobody"));
    Assertions.assertTrue(users.isEmpty());
  }
}
