package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.GraphQLTestWithCurrentUser;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
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
public class ProfileDatafetcherTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private ProfileData profileData;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    profileData = new ProfileData(
      user.getId(),
      user.getUsername(),
      user.getBio(),
      user.getImage(),
      false
    );
  }

  @Test
  public void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq(user.getUsername()), any())).thenReturn(Optional.of(profileData));

    String query = String.format("{ profile(username: \"%s\") { profile { username bio image following } } }", user.getUsername());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.profile", Collections.emptyMap());
    assertNotNull(result);
    
    Map<String, Object> profile = (Map<String, Object>) result.get("profile");
    assertEquals(user.getUsername(), profile.get("username"));
    assertEquals(user.getBio(), profile.get("bio"));
    assertEquals(user.getImage(), profile.get("image"));
  }

  @Test
  public void should_return_error_for_nonexistent_profile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query = "{ profile(username: \"nonexistent\") { profile { username } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(query, "data", Collections.emptyMap());
      fail("Should throw ResourceNotFoundException");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("ResourceNotFoundException") || 
                 e.getMessage().contains("not found"));
    }
  }
}
