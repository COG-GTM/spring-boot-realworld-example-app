package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import graphql.ExecutionResult;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

public class UserMutationTest extends GraphQLTestBase {

  @BeforeEach
  public void setUp() {
    when(jwtService.toToken(any())).thenReturn("generated-token");
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    user.getId(), user.getUsername(), user.getBio(), user.getImage(), false)));
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \""
                + user.getEmail()
                + "\", username: \""
                + user.getUsername()
                + "\", password: \"123\"}) { ... on UserPayload { user { email username token"
                + " profile { username } } } ... on Error { message } } }");

    assertThat(context.<String>read("data.createUser.user.email")).isEqualTo(user.getEmail());
    assertThat(context.<String>read("data.createUser.user.username")).isEqualTo(user.getUsername());
    assertThat(context.<String>read("data.createUser.user.token")).isEqualTo("generated-token");
    assertThat(context.<String>read("data.createUser.user.profile.username"))
        .isEqualTo(user.getUsername());

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo(user.getEmail());
    assertThat(captor.getValue().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  public void should_return_error_payload_when_create_user_is_invalid() {
    Set<ConstraintViolation<InvalidInput>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(new InvalidInput());
    when(userService.createUser(any())).thenThrow(new ConstraintViolationException(violations));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"\", username: \"u\", password: \"p\"}) {"
                + " __typename ... on Error { message errors { key value } } } }");

    assertThat(context.<String>read("data.createUser.__typename")).isEqualTo("Error");
    assertThat(context.<String>read("data.createUser.message")).isEqualTo("BAD_REQUEST");
    assertThat(context.<String>read("data.createUser.errors[0].key")).isEqualTo("email");
    assertThat(context.<List<String>>read("data.createUser.errors[0].value"))
        .containsExactly("can't be empty");
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { login(email: \""
                + user.getEmail()
                + "\", password: \"123\") { user { email username token } } }");

    assertThat(context.<String>read("data.login.user.email")).isEqualTo(user.getEmail());
    assertThat(context.<String>read("data.login.user.token")).isEqualTo("generated-token");
  }

  @Test
  public void should_return_unauthenticated_error_with_wrong_password() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \""
                + user.getEmail()
                + "\", password: \"wrong\") { user { token } } }");

    assertThat(result.getErrors()).hasSize(1);
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertThat(extensions.get("errorType")).isEqualTo("UNAUTHENTICATED");
  }

  @Test
  public void should_return_unauthenticated_error_with_unknown_email() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"nobody@test.com\", password: \"123\") { user { token } }"
                + " }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions().get("errorType"))
        .isEqualTo("UNAUTHENTICATED");
  }

  @Test
  public void should_update_current_user() {
    loginAs(user);

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateUser(changes: {bio: \"new bio\", image: \"new image\"}) { user {"
                + " email username } } }");

    assertThat(context.<String>read("data.updateUser.user.email")).isEqualTo(user.getEmail());
    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isEqualTo(user);
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new image");
  }

  @Test
  public void should_return_null_when_updating_user_anonymously() {
    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateUser(changes: {bio: \"new bio\"}) { user { email } } }");

    assertThat(context.<Object>read("data.updateUser")).isNull();
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_query_me_with_token() {
    loginAs(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getBio(),
                    user.getImage())));
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token my-token");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ me { email username token profile { username } } }", Map.of(), headers);

    assertThat(context.<String>read("data.me.email")).isEqualTo(user.getEmail());
    assertThat(context.<String>read("data.me.username")).isEqualTo(user.getUsername());
    assertThat(context.<String>read("data.me.token")).isEqualTo("my-token");
    assertThat(context.<String>read("data.me.profile.username")).isEqualTo(user.getUsername());
  }

  @Test
  public void should_return_null_me_for_anonymous_user() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token my-token");

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext("{ me { email } }", Map.of(), headers);

    assertThat(context.<Object>read("data.me")).isNull();
  }

  static class InvalidInput {
    @NotBlank(message = "can't be empty")
    private String email = "";
  }
}
