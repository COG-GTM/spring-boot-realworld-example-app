package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;


@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
public class ProfileDatafetcherTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean ProfileQueryService profileQueryService;

  private ProfileData profileData;

  @BeforeEach
  @Override
  public void setUpUser() {
    super.setUpUser();
    user = new User("test@example.com", "testuser", "password", "Test bio", "https://example.com/image.jpg");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
  }

  @Test
  
  public void testQueryProfile() {
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileData));

    String query = "{ profile(username: \"testuser\") { profile { username bio image following } } }";
    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    String bio = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.bio");
    String image = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.image");
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertEquals("testuser", username);
    assertEquals("Test bio", bio);
    assertEquals("https://example.com/image.jpg", image);
    assertEquals(false, following);
  }

  @Test
  
  public void testQueryProfileNotFound() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile");
    });
  }

  @Test
  
  public void testQueryProfileWithFollowing() {
    ProfileData followingProfileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), true);
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(followingProfileData));

    String query = "{ profile(username: \"testuser\") { profile { username following } } }";
    Boolean following = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.following");

    assertEquals(true, following);
  }

  @Test
  
  public void testQueryProfileWithNullBio() {
    ProfileData profileWithNullBio = new ProfileData(user.getId(), user.getUsername(), null, user.getImage(), false);
    when(profileQueryService.findByUsername(eq("testuser"), any())).thenReturn(Optional.of(profileWithNullBio));

    String query = "{ profile(username: \"testuser\") { profile { username bio } } }";
    String username = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile.profile.username");
    
    assertEquals("testuser", username);
  }
}
