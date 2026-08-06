package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ProfileDatafetcher.class})
public class ProfileDatafetcherTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User target;

  @BeforeEach
  void setUp() {
    target = TestHelper.userFixture("target");
    authenticateAnonymously();
  }

  @Test
  public void should_query_profile_of_another_user() {
    when(profileQueryService.findByUsername(eq(target.getUsername()), isNull()))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(target)));

    Map<String, Object> profile =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \""
                + target.getUsername()
                + "\") { profile { username bio image following } } }",
            "data.profile.profile");

    assertThat(profile.get("username")).isEqualTo(target.getUsername());
    assertThat(profile.get("bio")).isEqualTo(target.getBio());
    assertThat(profile.get("image")).isEqualTo(target.getImage());
    assertThat(profile.get("following")).isEqualTo(false);
  }

  @Test
  public void should_mark_profile_as_followed_by_current_user() {
    User current = TestHelper.userFixture("current");
    authenticate(current);
    ProfileData following =
        new ProfileData(
            target.getId(), target.getUsername(), target.getBio(), target.getImage(), true);
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(current)))
        .thenReturn(Optional.of(following));

    Boolean isFollowing =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"" + target.getUsername() + "\") { profile { following } } }",
            "data.profile.profile.following");

    assertThat(isFollowing).isTrue();
  }

  @Test
  public void should_fail_when_profile_does_not_exist() {
    when(profileQueryService.findByUsername(eq("ghost"), isNull())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"ghost\") { profile { username } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }
}
