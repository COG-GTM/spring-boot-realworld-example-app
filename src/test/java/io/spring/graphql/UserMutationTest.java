package io.spring.graphql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.JsonPath;
import io.spring.application.data.UserData;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
public class UserMutationTest extends GraphQLTestHelper {

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @MockBean private UserReadService userReadService;

  @MockBean private UserService userService;

  @Autowired private PasswordEncoder passwordEncoder;

  private String defaultAvatar;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
  }

  @Test
  public void should_create_user_successfully() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String password = "password123";

    User user = new User(email, username, "encodedPassword", "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);

    when(userService.createUser(any())).thenReturn(user);
    when(userReadService.findById(any())).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("test-token");
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.empty());
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation =
        "mutation CreateUser($input: CreateUserInput!) {\n"
            + "  createUser(input: $input) {\n"
            + "    ... on UserPayload {\n"
            + "      user {\n"
            + "        email\n"
            + "        username\n"
            + "        token\n"
            + "      }\n"
            + "    }\n"
            + "    ... on Error {\n"
            + "      message\n"
            + "      errors {\n"
            + "        key\n"
            + "        value\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", password);
    variables.put("input", input);

    MvcResult result = executeGraphQL(mutation, variables);
    String content = result.getResponse().getContentAsString();

    assertFalse(hasErrors(result));
    assertThat(JsonPath.read(content, "$.data.createUser.user.email"), equalTo(email));
    assertThat(JsonPath.read(content, "$.data.createUser.user.username"), equalTo(username));
    assertThat(JsonPath.read(content, "$.data.createUser.user.token"), notNullValue());

    verify(userService).createUser(any());
  }

  @Test
  public void should_login_successfully() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String password = "password123";

    User user = new User(email, username, passwordEncoder.encode(password), "", defaultAvatar);
    UserData userData = new UserData(user.getId(), email, username, "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);
    when(jwtService.toToken(any())).thenReturn("test-token");

    String mutation =
        "mutation Login($email: String!, $password: String!) {\n"
            + "  login(email: $email, password: $password) {\n"
            + "    user {\n"
            + "      email\n"
            + "      username\n"
            + "      token\n"
            + "    }\n"
            + "  }\n"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    MvcResult result = executeGraphQL(mutation, variables);
    String content = result.getResponse().getContentAsString();

    assertFalse(hasErrors(result));
    assertThat(JsonPath.read(content, "$.data.login.user.email"), equalTo(email));
    assertThat(JsonPath.read(content, "$.data.login.user.username"), equalTo(username));
    assertThat(JsonPath.read(content, "$.data.login.user.token"), notNullValue());
  }

  @Test
  public void should_fail_login_with_wrong_password() throws Exception {
    String email = "john@example.com";
    String username = "johndoe";
    String correctPassword = "password123";
    String wrongPassword = "wrongpassword";

    User user = new User(email, username, passwordEncoder.encode(correctPassword), "", defaultAvatar);

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

    String mutation =
        "mutation Login($email: String!, $password: String!) {\n"
            + "  login(email: $email, password: $password) {\n"
            + "    user {\n"
            + "      email\n"
            + "      username\n"
            + "      token\n"
            + "    }\n"
            + "  }\n"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", wrongPassword);

    MvcResult result = executeGraphQL(mutation, variables);

    assertTrue(hasErrors(result));
  }

  @Test
  public void should_fail_login_with_nonexistent_email() throws Exception {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.empty());

    String mutation =
        "mutation Login($email: String!, $password: String!) {\n"
            + "  login(email: $email, password: $password) {\n"
            + "    user {\n"
            + "      email\n"
            + "      username\n"
            + "      token\n"
            + "    }\n"
            + "  }\n"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("email", email);
    variables.put("password", password);

    MvcResult result = executeGraphQL(mutation, variables);

    assertTrue(hasErrors(result));
  }

}
