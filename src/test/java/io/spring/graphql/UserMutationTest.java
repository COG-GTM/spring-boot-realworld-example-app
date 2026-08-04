package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class, MeDatafetcher.class})
class UserMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private PasswordEncoder encryptService;
  @MockBean private io.spring.application.user.UserService userService;

  // Collaborators for MeDatafetcher, imported to resolve UserPayload.user.
  @MockBean private io.spring.application.UserQueryService userQueryService;
  @MockBean private JwtService jwtService;

  @Test
  void should_create_user_and_resolve_payload_user() {
    User created = new User("new@example.com", "newuser", "encoded", "", "");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(created);
    when(jwtService.toToken(eq(created))).thenReturn("jwt-token");

    String query =
        "mutation { createUser(input: {email: \"new@example.com\", username: \"newuser\","
            + " password: \"secret123\"}) { ... on UserPayload { user { email username token } } } }";

    Map<String, Object> user =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.createUser.user");

    assertThat(user.get("email")).isEqualTo("new@example.com");
    assertThat(user.get("username")).isEqualTo("newuser");
    assertThat(user.get("token")).isEqualTo("jwt-token");
  }

  @Test
  void should_return_error_union_when_registration_violates_constraints() {
    ConstraintViolationException cve =
        new ConstraintViolationException(buildViolation("username", "already exist"));
    when(userService.createUser(any(RegisterParam.class))).thenThrow(cve);

    String query =
        "mutation { createUser(input: {email: \"new@example.com\", username: \"taken\","
            + " password: \"secret123\"}) { ... on Error { message errors { key value } } } }";

    String message = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.createUser.message");
    java.util.List<String> values =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.createUser.errors[0].value");

    assertThat(message).isEqualTo("BAD_REQUEST");
    assertThat(values).contains("already exist");
  }

  @Test
  void should_login_with_valid_credentials() {
    User user = new User("john@example.com", "john", "hashed", "", "");
    when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("secret"), eq("hashed"))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("login-token");

    String query =
        "mutation { login(email: \"john@example.com\", password: \"secret\") { user { username token } } }";

    Map<String, Object> data = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.login.user");

    assertThat(data.get("username")).isEqualTo("john");
    assertThat(data.get("token")).isEqualTo("login-token");
  }

  @Test
  void should_fail_login_with_invalid_credentials() {
    when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());

    String query =
        "mutation { login(email: \"john@example.com\", password: \"wrong\") { user { username } } }";

    ExecutionResult result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void should_update_authenticated_user() {
    User current = new User("john@example.com", "john", "hashed", "old bio", "");
    authenticate(current);
    when(jwtService.toToken(eq(current))).thenReturn("updated-token");

    String query =
        "mutation { updateUser(changes: {email: \"changed@example.com\", bio: \"new bio\"})"
            + " { user { email username token } } }";

    Map<String, Object> user =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.updateUser.user");

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    Mockito.verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(current);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("changed@example.com");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    // The token/username come from the (mocked, unchanged) current principal.
    assertThat(user.get("username")).isEqualTo("john");
    assertThat(user.get("token")).isEqualTo("updated-token");
  }

  @Test
  void should_return_null_when_updating_unauthenticated_user() {
    anonymous();

    String query = "mutation { updateUser(changes: {bio: \"new bio\"}) { user { username } } }";

    Object updateUser = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.updateUser");

    assertThat(updateUser).isNull();
  }

  private Set<ConstraintViolation<?>> buildViolation(String field, String message) {
    ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    javax.validation.Path path = Mockito.mock(javax.validation.Path.class);
    when(path.toString()).thenReturn(field);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    Mockito.<Class<?>>when(violation.getRootBeanClass()).thenReturn(User.class);
    javax.validation.metadata.ConstraintDescriptor<?> descriptor =
        Mockito.mock(javax.validation.metadata.ConstraintDescriptor.class);
    when(violation.getConstraintDescriptor())
        .thenReturn((javax.validation.metadata.ConstraintDescriptor) descriptor);
    java.lang.annotation.Annotation annotation =
        Mockito.mock(java.lang.annotation.Annotation.class);
    Mockito.<Class<? extends java.lang.annotation.Annotation>>when(annotation.annotationType())
        .thenReturn((Class) javax.validation.constraints.NotNull.class);
    when(descriptor.getAnnotation()).thenReturn((java.lang.annotation.Annotation) annotation);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    return violations;
  }
}
