package io.spring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.api.TestWithCurrentUser;
import io.spring.graphql.RelationMutation;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RelationMutationTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User targetUser;
  private ProfileData targetProfile;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    targetUser = new User("target@example.com", "target", "123", "", "");
    targetProfile = new ProfileData(targetUser.getId(), "target", "", "", false);
  }

  @Test
  public void should_follow_user_success() {
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(targetUser));
    when(profileQueryService.findByUsername(eq("target"), any())).thenReturn(Optional.of(targetProfile));

    String mutation = "mutation { followUser(username: \"target\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.followUser", Collections.emptyMap());
    assertNotNull(result);
    
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertEquals("target", profile.get("username"));
    
    verify(userRepository).saveRelation(any(FollowRelation.class));
  }

  @Test
  public void should_unfollow_user_success() {
    FollowRelation followRelation = new FollowRelation(user.getId(), targetUser.getId());
    when(userRepository.findByUsername(eq("target"))).thenReturn(Optional.of(targetUser));
    when(userRepository.findRelation(eq(user.getId()), eq(targetUser.getId()))).thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq("target"), any())).thenReturn(Optional.of(targetProfile));

    String mutation = "mutation { unfollowUser(username: \"target\") { profile { username following } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfollowUser", Collections.emptyMap());
    assertNotNull(result);
    
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertEquals("target", profile.get("username"));
    
    verify(userRepository).removeRelation(eq(followRelation));
  }

  @Test
  public void should_fail_to_follow_nonexistent_user() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { followUser(username: \"nonexistent\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data", Collections.emptyMap());
      fail("Should throw ResourceNotFoundException");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("ResourceNotFoundException") || 
                 e.getMessage().contains("not found"));
    }
  }
}
