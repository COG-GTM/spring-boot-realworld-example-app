package io.spring.infrastructure.user;

import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(MyBatisUserRepository.class)
public class MyBatisUserRepositoryTest extends DbTestBase {
  @Autowired private UserRepository userRepository;
  @Autowired private UserRelationshipQueryService userRelationshipQueryService;
  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@163.com", "aisensiy", "123", "", "default");
  }

  @Test
  public void should_save_and_fetch_user_success() {
    userRepository.save(user);
    Optional<User> userOptional = userRepository.findByUsername("aisensiy");
    Assertions.assertEquals(userOptional.get(), user);
    Optional<User> userOptional2 = userRepository.findByEmail("aisensiy@163.com");
    Assertions.assertEquals(userOptional2.get(), user);
  }

  @Test
  public void should_update_user_success() {
    String newEmail = "newemail@email.com";
    user.update(newEmail, "", "", "", "");
    userRepository.save(user);
    Optional<User> optional = userRepository.findByUsername(user.getUsername());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getEmail(), newEmail);

    String newUsername = "newUsername";
    user.update("", newUsername, "", "", "");
    userRepository.save(user);
    optional = userRepository.findByEmail(user.getEmail());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getUsername(), newUsername);
    Assertions.assertEquals(optional.get().getImage(), user.getImage());
  }

  @Test
  public void should_find_user_by_id_success() {
    userRepository.save(user);

    Optional<User> optional = userRepository.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get(), user);
  }

  @Test
  public void should_get_empty_optional_for_unknown_user() {
    Assertions.assertFalse(userRepository.findById("not-exists-id").isPresent());
    Assertions.assertFalse(userRepository.findByUsername("not-exists-username").isPresent());
    Assertions.assertFalse(userRepository.findByEmail("not-exists@email.com").isPresent());
  }

  @Test
  public void should_update_existing_user_instead_of_inserting_a_new_one() {
    userRepository.save(user);

    String originalUsername = user.getUsername();
    user.update("updated@email.com", "updatedUsername", "", "new bio", "new image");
    userRepository.save(user);

    Assertions.assertFalse(userRepository.findByUsername(originalUsername).isPresent());
    Assertions.assertFalse(userRepository.findByEmail("aisensiy@163.com").isPresent());

    Optional<User> optional = userRepository.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getUsername(), "updatedUsername");
    Assertions.assertEquals(optional.get().getEmail(), "updated@email.com");
    Assertions.assertEquals(optional.get().getBio(), "new bio");
    Assertions.assertEquals(optional.get().getImage(), "new image");
  }

  @Test
  public void should_update_only_the_non_empty_fields() {
    userRepository.save(user);

    user.update("", "", "", "new bio", "");
    userRepository.save(user);

    Optional<User> optional = userRepository.findById(user.getId());
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertEquals(optional.get().getBio(), "new bio");
    Assertions.assertEquals(optional.get().getUsername(), "aisensiy");
    Assertions.assertEquals(optional.get().getEmail(), "aisensiy@163.com");
    Assertions.assertEquals(optional.get().getImage(), "default");
  }

  @Test
  public void should_create_new_user_follow_success() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);

    FollowRelation followRelation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(followRelation);
    Assertions.assertTrue(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }

  @Test
  public void should_unfollow_user_success() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);

    FollowRelation followRelation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(followRelation);

    userRepository.removeRelation(followRelation);
    Assertions.assertFalse(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }

  @Test
  public void should_not_duplicate_follow_relation_when_saved_twice() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);
    userRepository.save(user);

    FollowRelation followRelation = new FollowRelation(user.getId(), other.getId());
    userRepository.saveRelation(followRelation);
    userRepository.saveRelation(followRelation);

    Assertions.assertEquals(1, userRelationshipQueryService.followedUsers(user.getId()).size());
  }

  @Test
  public void should_get_empty_relation_when_user_is_not_followed() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);
    userRepository.save(user);

    Assertions.assertFalse(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }

  @Test
  public void should_do_nothing_when_removing_a_relation_that_does_not_exist() {
    User other = new User("other@example.com", "other", "123", "", "");
    userRepository.save(other);
    userRepository.save(user);

    userRepository.removeRelation(new FollowRelation(user.getId(), other.getId()));

    Assertions.assertFalse(userRepository.findRelation(user.getId(), other.getId()).isPresent());
  }
}
