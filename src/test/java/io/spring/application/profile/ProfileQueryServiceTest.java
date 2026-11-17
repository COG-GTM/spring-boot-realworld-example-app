package io.spring.application.profile;

import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({ProfileQueryService.class, MyBatisUserRepository.class})
public class ProfileQueryServiceTest extends DbTestBase {
  @Autowired private ProfileQueryService profileQueryService;
  @Autowired private UserRepository userRepository;

  @Test
  public void should_fetch_profile_success() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    User profileUser = new User("p@test.com", "p", "123", "", "");
    userRepository.save(profileUser);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername(profileUser.getUsername(), currentUser);
    Assertions.assertTrue(optional.isPresent());
  }

  @Test
  public void should_fetch_profiles_in_batch_with_following_state() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    User followed = new User("f@test.com", "f", "123", "bio", "img");
    User other = new User("o@test.com", "o", "123", "", "");
    userRepository.save(currentUser);
    userRepository.save(followed);
    userRepository.save(other);
    userRepository.saveRelation(new FollowRelation(currentUser.getId(), followed.getId()));

    Map<String, ProfileData> profiles =
        profileQueryService.findByUsernames(Arrays.asList("f", "o", "missing"), currentUser);

    Assertions.assertEquals(2, profiles.size());
    Assertions.assertTrue(profiles.get("f").isFollowing());
    Assertions.assertEquals("bio", profiles.get("f").getBio());
    Assertions.assertFalse(profiles.get("o").isFollowing());
  }

  @Test
  public void should_return_empty_map_for_empty_usernames() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);

    Map<String, ProfileData> profiles =
        profileQueryService.findByUsernames(Collections.emptyList(), currentUser);

    Assertions.assertTrue(profiles.isEmpty());
  }

  @Test
  public void should_fetch_profiles_in_batch_for_anonymous_user() {
    User followed = new User("f@test.com", "f", "123", "bio", "img");
    User follower = new User("o@test.com", "o", "123", "", "");
    userRepository.save(followed);
    userRepository.save(follower);
    userRepository.saveRelation(new FollowRelation(follower.getId(), followed.getId()));

    Map<String, ProfileData> profiles =
        profileQueryService.findByUsernames(Arrays.asList("f", "o"), null);

    Assertions.assertEquals(2, profiles.size());
    Assertions.assertFalse(profiles.get("f").isFollowing());
    Assertions.assertFalse(profiles.get("o").isFollowing());
    Assertions.assertEquals("img", profiles.get("f").getImage());
  }

  @Test
  public void should_return_empty_map_when_no_usernames_exist() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);

    Map<String, ProfileData> profiles =
        profileQueryService.findByUsernames(Arrays.asList("x", "y"), currentUser);

    Assertions.assertTrue(profiles.isEmpty());
  }
}
