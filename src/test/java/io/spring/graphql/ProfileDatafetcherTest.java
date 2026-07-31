package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.api.exception.ResourceNotFoundException;
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
class ProfileDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private User target;

  @BeforeEach
  void setUp() {
    target = userFixture("jane");
  }

  @Test
  void should_query_profile_by_username() {
    anonymous();
    when(profileQueryService.findByUsername(eq(target.getUsername()), isNull()))
        .thenReturn(Optional.of(profileDataOf(target, false)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "{ profile(username: \"%s\") { profile { username bio image following } } }",
                target.getUsername()));

    assertThat(context.read("data.profile.profile.username", String.class))
        .isEqualTo(target.getUsername());
    assertThat(context.read("data.profile.profile.bio", String.class)).isEqualTo(target.getBio());
    assertThat(context.read("data.profile.profile.image", String.class))
        .isEqualTo(target.getImage());
    assertThat(context.read("data.profile.profile.following", Boolean.class)).isFalse();
  }

  @Test
  void should_use_current_user_to_resolve_following_flag() {
    User current = userFixture("john");
    authenticate(current);
    when(profileQueryService.findByUsername(eq(target.getUsername()), eq(current)))
        .thenReturn(Optional.of(profileDataOf(target, true)));

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { following } } }", target.getUsername()),
            "data.profile.profile.following");

    verify(profileQueryService).findByUsername(eq(target.getUsername()), eq(current));
    assertThat(following).isTrue();
  }

  @Test
  void should_return_error_when_profile_not_found() {
    anonymous();
    when(profileQueryService.findByUsername(eq("ghost"), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"ghost\") { profile { username } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
  }

  private ProfileData profileDataOf(User user, boolean following) {
    return new ProfileData(
        user.getId(), user.getUsername(), user.getBio(), user.getImage(), following);
  }
}
