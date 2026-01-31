package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private io.spring.application.user.UserService userService;
  @MockBean private UserRepository userRepository;
  @MockBean private UserReadService userReadService;
  @MockBean private JwtService jwtService;
  @MockBean private PasswordEncoder passwordEncoder;

  private String email;
  private String username;
  private String password;
  private User user;

  @BeforeEach
  public void setUp() {
    email = "john@jacob.com";
    username = "johnjacob";
    password = "123";
    user = new User(email, username, password, "", "");
  }

  @Test
  public void should_create_user_success() {
    when(userService.createUser(any(io.spring.application.user.RegisterParam.class))).thenReturn(user);
    when(jwtService.toToken(any(User.class))).thenReturn("token");
    
    UserData userData = new UserData(user.getId(), email, username, "", "");
    when(userReadService.findById(any())).thenReturn(userData);

    String mutation = String.format(
      "mutation { createUser(input: {email: \"%s\", username: \"%s\", password: \"%s\"}) { ... on UserPayload { user { email username token } } } }",
      email, username, password
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createUser", Collections.emptyMap());
    assertNotNull(result);
    
    Map<String, Object> userResult = (Map<String, Object>) result.get("user");
    assertEquals(email, userResult.get("email"));
    assertEquals(username, userResult.get("username"));
    assertNotNull(userResult.get("token"));
  }

  @Test
  public void should_login_success() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(jwtService.toToken(any(User.class))).thenReturn("token");
    
    UserData userData = new UserData(user.getId(), email, username, "", "");
    when(userReadService.findById(any())).thenReturn(userData);

    String mutation = String.format(
      "mutation { login(email: \"%s\", password: \"%s\") { user { email username token } } }",
      email, password
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login", Collections.emptyMap());
    assertNotNull(result);
    
    Map<String, Object> userResult = (Map<String, Object>) result.get("user");
    assertEquals(email, userResult.get("email"));
    assertEquals(username, userResult.get("username"));
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    String mutation = String.format(
      "mutation { login(email: \"%s\", password: \"wrongpassword\") { user { email } } }",
      email
    );

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data", Collections.emptyMap());
      fail("Should throw authentication error");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof InvalidAuthenticationException || 
                 e.getMessage().contains("invalid email or password"));
    }
  }
}
