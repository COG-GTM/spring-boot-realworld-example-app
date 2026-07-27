package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.application.data.ProfileData;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProfileDatafetcherTest extends GraphQLTestBase {

  @Test
  public void should_query_profile_by_username() {
    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(Optional.of(new ProfileData(user.getId(), username, "bio", defaultAvatar, true)));

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { username bio image following } } }",
                username),
            "data.profile.profile.following");

    Assertions.assertTrue(following);
  }

  @Test
  public void should_query_profile_when_anonymous() {
    logout();
    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(Optional.of(new ProfileData(user.getId(), username, "bio", defaultAvatar, false)));

    String returned =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format("{ profile(username: \"%s\") { profile { username } } }", username),
            "data.profile.profile.username");

    Assertions.assertEquals(username, returned);
  }

  @Test
  public void should_return_error_when_profile_not_found() {
    when(profileQueryService.findByUsername(any(), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ profile(username: \"ghost\") { profile { username } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }
}
