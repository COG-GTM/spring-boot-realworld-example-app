package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private UserRepository userRepository;

  @AfterEach
  public void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_successfully() {
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"a@b.com\", username: \"alice\", password: \"pw\"}) { __typename ... on UserPayload { user { email username token } } } }");

    assertThat((String) context.read("data.createUser.__typename")).isEqualTo("UserPayload");
    assertThat((String) context.read("data.createUser.user.email")).isEqualTo("a@b.com");
    assertThat((String) context.read("data.createUser.user.username")).isEqualTo("alice");
    assertThat((String) context.read("data.createUser.user.token")).isNotBlank();
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_return_error_data_for_invalid_email() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createUser(input: {email: \"not-an-email\", username: \"alice\", password: \"pw\"}) { __typename ... on Error { message errors { key value } } } }");

    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<String, Object> createUser = (Map<String, Object>) data.get("createUser");
    assertThat(createUser.get("__typename")).isEqualTo("Error");
    assertThat(createUser.get("message")).isEqualTo("BAD_REQUEST");
    List<Map<String, Object>> errors = (List<Map<String, Object>>) createUser.get("errors");
    assertThat(errors.stream().map(e -> e.get("key"))).contains("email");
  }

  @Test
  public void should_login_successfully() {
    User user = new User("a@b.com", "alice", passwordEncoder.encode("pw"), "", "default-image");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { login(password: \"pw\", email: \"a@b.com\") { user { email username token } } }");

    assertThat((String) context.read("data.login.user.email")).isEqualTo("a@b.com");
    assertThat((String) context.read("data.login.user.username")).isEqualTo("alice");
    assertThat((String) context.read("data.login.user.token")).isNotBlank();
  }

  @Test
  public void should_return_unauthenticated_error_for_wrong_password() {
    User user = new User("a@b.com", "alice", passwordEncoder.encode("pw"), "", "default-image");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(password: \"wrong\", email: \"a@b.com\") { user { email } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions().get("errorType"))
        .isEqualTo("UNAUTHENTICATED");
    assertThat(result.getErrors().get(0).getPath()).isEqualTo(List.of("login"));
  }
}
