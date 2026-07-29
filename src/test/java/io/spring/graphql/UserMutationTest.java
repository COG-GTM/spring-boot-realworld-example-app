package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  @MockBean private UserService userService;

  @MockBean private io.spring.application.UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@test.com", "john", "encoded-password", "bio", "image");
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_a_user() {
    when(userService.createUser(any())).thenReturn(user);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"john@test.com\", username: \"john\", password:"
                + " \"secret\"}) { ... on UserPayload { user { email username token } } } }");

    assertThat(result.<String>read("data.createUser.user.email")).isEqualTo(user.getEmail());
    assertThat(result.<String>read("data.createUser.user.username")).isEqualTo(user.getUsername());
    assertThat(result.<String>read("data.createUser.user.token")).isEqualTo("jwt-token");

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("john@test.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("john");
    assertThat(captor.getValue().getPassword()).isEqualTo("secret");
  }

  @Test
  public void should_return_an_error_payload_when_registration_is_invalid() {
    when(userService.createUser(any())).thenThrow(constraintViolationException());

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { createUser(input: {email: \"not-an-email\", username: \"\", password:"
                + " \"secret\"}) { ... on Error { message errors { key value } } } }");

    assertThat(result.<String>read("data.createUser.message")).isEqualTo("BAD_REQUEST");
    assertThat(result.<List<String>>read("data.createUser.errors[*].key"))
        .containsExactlyInAnyOrder("email", "username");
    assertThat(result.<List<String>>read("data.createUser.errors[?(@.key == 'email')].value[0]"))
        .containsExactly("should be an email");
  }

  @Test
  public void should_log_a_user_in() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("secret"), eq(user.getPassword()))).thenReturn(true);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { login(email: \"john@test.com\", password: \"secret\") { user { email"
                + " username token } } }");

    assertThat(result.<String>read("data.login.user.email")).isEqualTo(user.getEmail());
    assertThat(result.<String>read("data.login.user.token")).isEqualTo("jwt-token");
  }

  @Test
  public void should_reject_a_login_with_a_wrong_password() {
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"john@test.com\", password: \"wrong\") { user { email } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).isEqualTo("invalid email or password");
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", "UNAUTHENTICATED");
  }

  @Test
  public void should_reject_a_login_of_an_unknown_email() {
    when(userRepository.findByEmail(eq("ghost@test.com"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"ghost@test.com\", password: \"secret\") { user { email } }"
                + " }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", "UNAUTHENTICATED");
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  public void should_update_the_current_user() {
    authenticate(user);

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { updateUser(changes: {email: \"new@test.com\", username: \"new\", bio: \"new"
                + " bio\", image: \"new image\", password: \"new password\"}) { user { email token }"
                + " } }");

    assertThat(result.<String>read("data.updateUser.user.email")).isEqualTo(user.getEmail());
    assertThat(result.<String>read("data.updateUser.user.token")).isEqualTo("jwt-token");

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isSameAs(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@test.com");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("new");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new image");
    assertThat(captor.getValue().getParam().getPassword()).isEqualTo("new password");
  }

  @Test
  public void should_return_null_when_updating_a_user_anonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateUser(changes: {email: \"new@test.com\"}) { user { email } } }");

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.<java.util.Map<String, Object>>getData()).containsEntry("updateUser", null);
    verify(userService, never()).updateUser(any());
  }

  private ConstraintViolationException constraintViolationException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Registration>> violations =
        validator.validate(new Registration("not-an-email", ""));
    return new ConstraintViolationException(violations);
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private static class Registration {
    @Email(message = "should be an email")
    private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    Registration(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }
}
