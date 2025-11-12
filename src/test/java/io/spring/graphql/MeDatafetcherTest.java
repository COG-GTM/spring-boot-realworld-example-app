package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      MeDatafetcher.class
    })
public class MeDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  private User user;
  private UserData userData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @Test
  public void should_query_current_user() {
    when(userQueryService.findById(any())).thenReturn(Optional.of(userData));
    when(jwtService.toToken(any())).thenReturn("test-token");

    String query =
        "query { "
            + "  me { "
            + "    email "
            + "    username "
            + "    token "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> me = (Map<String, Object>) data.get("me");
    assertNotNull(me);
    assertEquals("test@example.com", me.get("email"));
    assertEquals("testuser", me.get("username"));
  }

  @Test
  public void should_return_null_when_user_not_authenticated() {
    SecurityContextHolder.clearContext();

    String query =
        "query { "
            + "  me { "
            + "    email "
            + "    username "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
  }

  @Test
  public void should_query_current_user_with_profile() {
    when(userQueryService.findById(any())).thenReturn(Optional.of(userData));
    when(jwtService.toToken(any())).thenReturn("test-token");

    String query =
        "query { "
            + "  me { "
            + "    email "
            + "    username "
            + "    profile { "
            + "      username "
            + "      bio "
            + "      image "
            + "    } "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> me = (Map<String, Object>) data.get("me");
    assertNotNull(me);
    assertEquals("test@example.com", me.get("email"));
    assertEquals("testuser", me.get("username"));
  }
}
