package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.graphql.types.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class MeDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @Test
  public void should_get_current_user_when_authenticated() {
    setAuthenticatedUser(user);

    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    String query = "query { me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.put("Authorization", Collections.singletonList("Token " + token));

    User result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.me", Collections.emptyMap(), User.class, headers);

    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getUsername()).isEqualTo(username);
  }

  @Test
  public void should_return_null_when_anonymous_user() {
    clearAuthentication();

    String query = "query { me { email username token } }";

    HttpHeaders headers = new HttpHeaders();
    headers.put("Authorization", Collections.singletonList("Token invalid"));

    Object result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.me", Collections.emptyMap(), Object.class, headers);

    assertThat(result).isNull();
  }
}
