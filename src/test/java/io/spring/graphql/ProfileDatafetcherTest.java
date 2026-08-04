package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ProfileDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
class ProfileDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ProfileQueryService profileQueryService;

  private static final String QUERY =
      "{ profile(username: \"targetuser\") { profile { username bio image following } } }";

  @Test
  void should_query_profile_for_authenticated_user() {
    User current = new User("me@example.com", "me", "123", "", "");
    authenticate(current);
    ProfileData profileData =
        new ProfileData("target-id", "targetuser", "a bio", "https://image/t.png", true);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(current)))
        .thenReturn(Optional.of(profileData));

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(QUERY);

    String path = "data.profile.profile";
    assertThat(context.read(path + ".username", String.class)).isEqualTo("targetuser");
    assertThat(context.read(path + ".bio", String.class)).isEqualTo("a bio");
    assertThat(context.read(path + ".image", String.class)).isEqualTo("https://image/t.png");
    assertThat(context.read(path + ".following", Boolean.class)).isTrue();
  }

  @Test
  void should_query_profile_for_anonymous_user_with_null_current() {
    anonymous();
    ProfileData profileData =
        new ProfileData("target-id", "targetuser", "a bio", "https://image/t.png", false);
    when(profileQueryService.findByUsername(eq("targetuser"), eq(null)))
        .thenReturn(Optional.of(profileData));

    Boolean following =
        dgsQueryExecutor.executeAndExtractJsonPath(QUERY, "data.profile.profile.following");
    assertThat(following).isFalse();
  }

  @Test
  void should_return_error_when_profile_not_found() {
    anonymous();
    when(profileQueryService.findByUsername(eq("targetuser"), any())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute(QUERY);

    assertSingleErrorFrom(result, ResourceNotFoundException.class);
    Map<String, Object> data = result.getData();
    assertThat(data.get("profile")).isNull();
  }
}
