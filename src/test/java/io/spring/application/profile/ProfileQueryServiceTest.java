package io.spring.application.profile;

import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
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
  public void should_return_empty_when_profile_not_found() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername("nonexistent-user", currentUser);
    Assertions.assertFalse(optional.isPresent());
  }

  @Test
  public void should_show_following_true_when_user_follows_profile() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);
    User profileUser = new User("p@test.com", "p", "123", "", "");
    userRepository.save(profileUser);

    FollowRelation followRelation = new FollowRelation(currentUser.getId(), profileUser.getId());
    userRepository.saveRelation(followRelation);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername(profileUser.getUsername(), currentUser);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertTrue(optional.get().isFollowing());
  }

  @Test
  public void should_show_following_false_when_user_does_not_follow_profile() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);
    User profileUser = new User("p@test.com", "p", "123", "", "");
    userRepository.save(profileUser);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername(profileUser.getUsername(), currentUser);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertFalse(optional.get().isFollowing());
  }

  @Test
  public void should_show_following_false_when_current_user_is_null() {
    User profileUser = new User("p@test.com", "p", "123", "", "");
    userRepository.save(profileUser);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername(profileUser.getUsername(), null);
    Assertions.assertTrue(optional.isPresent());
    Assertions.assertFalse(optional.get().isFollowing());
  }

  @Test
  public void should_handle_multiple_users_following_same_profile() {
    User follower1 = new User("f1@test.com", "follower1", "123", "", "");
    userRepository.save(follower1);
    User follower2 = new User("f2@test.com", "follower2", "123", "", "");
    userRepository.save(follower2);
    User follower3 = new User("f3@test.com", "follower3", "123", "", "");
    userRepository.save(follower3);
    User profileUser = new User("p@test.com", "p", "123", "", "");
    userRepository.save(profileUser);

    userRepository.saveRelation(new FollowRelation(follower1.getId(), profileUser.getId()));
    userRepository.saveRelation(new FollowRelation(follower2.getId(), profileUser.getId()));

    Optional<ProfileData> optionalForFollower1 =
        profileQueryService.findByUsername(profileUser.getUsername(), follower1);
    Assertions.assertTrue(optionalForFollower1.isPresent());
    Assertions.assertTrue(optionalForFollower1.get().isFollowing());

    Optional<ProfileData> optionalForFollower2 =
        profileQueryService.findByUsername(profileUser.getUsername(), follower2);
    Assertions.assertTrue(optionalForFollower2.isPresent());
    Assertions.assertTrue(optionalForFollower2.get().isFollowing());

    Optional<ProfileData> optionalForFollower3 =
        profileQueryService.findByUsername(profileUser.getUsername(), follower3);
    Assertions.assertTrue(optionalForFollower3.isPresent());
    Assertions.assertFalse(optionalForFollower3.get().isFollowing());
  }

  @Test
  public void should_return_correct_profile_data() {
    User currentUser = new User("a@test.com", "a", "123", "", "");
    userRepository.save(currentUser);
    User profileUser = new User("p@test.com", "profileuser", "123", "My bio", "http://image.url");
    userRepository.save(profileUser);

    Optional<ProfileData> optional =
        profileQueryService.findByUsername(profileUser.getUsername(), currentUser);
    Assertions.assertTrue(optional.isPresent());

    ProfileData profileData = optional.get();
    Assertions.assertEquals(profileUser.getId(), profileData.getId());
    Assertions.assertEquals("profileuser", profileData.getUsername());
    Assertions.assertEquals("My bio", profileData.getBio());
    Assertions.assertEquals("http://image.url", profileData.getImage());
  }
}
