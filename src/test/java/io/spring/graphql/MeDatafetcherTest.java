package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

@SpringBootTest(classes = {DgsAutoConfiguration.class, MeDatafetcher.class})
public class MeDatafetcherTest extends TestWithCurrentUser {

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

    String resultEmail =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.email", headers);
    String resultUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.username", headers);
    String resultToken =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me.token", headers);

    assertThat(resultEmail).isEqualTo(email);
    assertThat(resultUsername).isEqualTo(username);
    assertThat(resultToken).isEqualTo(token);
  }

  @Test
  public void should_return_null_when_not_authenticated() {
    setAnonymousAuthentication();

    String query = "query { me { email username } }";

    HttpHeaders headers = new HttpHeaders();
    headers.put("Authorization", Collections.singletonList("Token anonymous"));

    Object result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", headers);

    assertThat(result).isNull();
  }
}
