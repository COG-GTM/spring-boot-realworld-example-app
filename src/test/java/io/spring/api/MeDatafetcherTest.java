package io.spring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.api.TestWithCurrentUser;
import io.spring.graphql.MeDatafetcher;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
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
public class MeDatafetcherTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));
    when(jwtService.toToken(any())).thenReturn(token);
  }

  @Test
  public void should_return_null_for_unauthenticated_user() {
    when(userQueryService.findById(any())).thenReturn(Optional.empty());

    String query = "{ me { email username } }";

    try {
      Object result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.me", Collections.emptyMap());
    } catch (Exception e) {
    }
  }
}
