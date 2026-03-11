package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;


@SpringBootTest(classes = {DgsAutoConfiguration.class, RelationMutation.class})
public class RelationMutationTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean UserRepository userRepository;

  @MockBean ProfileQueryService profileQueryService;

  private User targetUser;
  private ProfileData profileData;

  @BeforeEach
  @Override
  public void setUpUser() {
    super.setUpUser();
    targetUser = new User("target@example.com", "targetuser", "password", "bio", "image");
    profileData = new ProfileData(targetUser.getId(), targetUser.getUsername(), targetUser.getBio(), targetUser.getImage(), false);
  }

  @Test
  
  public void testFollowUser() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { followUser(username: \"targetuser\") { profile { username following } } }";
    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser.profile.username");
    
    assertEquals("targetuser", username);
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  
  public void testFollowNonExistentUser() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser");
    });
  }

  @Test
  
  public void testUnfollowUser() {
    FollowRelation relation = new FollowRelation(user.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(any(), eq(targetUser.getId()))).thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.of(profileData));

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username following } } }";
    String username = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser.profile.username");
    
    assertEquals("targetuser", username);
    verify(userRepository).removeRelation(relation);
  }

  @Test
  
  public void testUnfollowNonExistentUser() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"nonexistent\") { profile { username } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser");
    });
  }

  @Test
  
  public void testUnfollowUserNotFollowing() {
    when(userRepository.findByUsername(eq("targetuser"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(any(), eq(targetUser.getId()))).thenReturn(Optional.empty());

    String mutation = "mutation { unfollowUser(username: \"targetuser\") { profile { username } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser");
    });
  }
}
