package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
public class RelationMutationTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private ProfileQueryService profileQueryService;

  private User currentUser;
  private User targetUser;
  private ProfileData targetProfileData;

  @BeforeEach
  void setUp() {
    currentUser = new User("current@example.com", "currentuser", "password", "bio", "image");
    targetUser = new User("target@example.com", "targetuser", "password", "Target bio", "target-image.jpg");

    targetProfileData = new ProfileData(
        targetUser.getId(),
        targetUser.getUsername(),
        targetUser.getBio(),
        targetUser.getImage(),
        true);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(currentUser, null, Arrays.asList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldFollowUser() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(targetProfileData));

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("targetuser");
    assertThat(result.get("following")).isEqualTo(true);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  void shouldUnfollowUser() {
    FollowRelation relation = new FollowRelation(currentUser.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.of(relation));

    ProfileData unfollowedProfile = new ProfileData(
        targetUser.getId(),
        targetUser.getUsername(),
        targetUser.getBio(),
        targetUser.getImage(),
        false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(currentUser)))
        .thenReturn(Optional.of(unfollowedProfile));

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile");

    assertThat(result).isNotNull();
    assertThat(result.get("username")).isEqualTo("targetuser");
    assertThat(result.get("following")).isEqualTo(false);
    verify(userRepository).removeRelation(eq(relation));
  }

  @Test
  void shouldFailToFollowWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToFollowNonExistentUser() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToUnfollowWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToUnfollowNonExistentRelation() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(currentUser.getId()), eq(targetUser.getId())))
        .thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
