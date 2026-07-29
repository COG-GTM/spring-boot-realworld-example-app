package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/smiley.jpg";

  @Mock private UserRepository userRepository;

  @Captor private ArgumentCaptor<User> userCaptor;

  private PasswordEncoder passwordEncoder;

  private UserService userService;

  private AutoCloseable mocks;

  private AnnotationConfigApplicationContext validationContext;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    passwordEncoder = new BCryptPasswordEncoder();
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (validationContext != null) {
      validationContext.close();
    }
    mocks.close();
  }

  @Test
  public void should_create_user_with_hashed_password() {
    RegisterParam registerParam = new RegisterParam("john@test.com", "john", "plain-password");

    User created = userService.createUser(registerParam);

    assertThat(created.getPassword()).isNotEqualTo("plain-password");
    assertThat(passwordEncoder.matches("plain-password", created.getPassword())).isTrue();
  }

  @Test
  public void should_create_user_with_default_image_and_empty_bio() {
    RegisterParam registerParam = new RegisterParam("john@test.com", "john", "plain-password");

    User created = userService.createUser(registerParam);

    assertThat(created.getEmail()).isEqualTo("john@test.com");
    assertThat(created.getUsername()).isEqualTo("john");
    assertThat(created.getBio()).isEmpty();
    assertThat(created.getImage()).isEqualTo(DEFAULT_IMAGE);
    assertThat(created.getId()).isNotBlank();
  }

  @Test
  public void should_persist_the_created_user_through_the_repository() {
    RegisterParam registerParam = new RegisterParam("john@test.com", "john", "plain-password");

    User created = userService.createUser(registerParam);

    verify(userRepository).save(userCaptor.capture());
    User saved = userCaptor.getValue();
    assertThat(saved).isSameAs(created);
    assertThat(saved.getUsername()).isEqualTo("john");
    assertThat(passwordEncoder.matches("plain-password", saved.getPassword())).isTrue();
  }

  @Test
  public void should_update_every_provided_field() {
    User targetUser = new User("old@test.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@test.com")
            .username("new")
            .password("new-password")
            .bio("new bio")
            .image("new image")
            .build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    verify(userRepository).save(userCaptor.capture());
    User saved = userCaptor.getValue();
    assertThat(saved).isSameAs(targetUser);
    assertThat(saved.getEmail()).isEqualTo("new@test.com");
    assertThat(saved.getUsername()).isEqualTo("new");
    assertThat(saved.getPassword()).isEqualTo("new-password");
    assertThat(saved.getBio()).isEqualTo("new bio");
    assertThat(saved.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_ignore_empty_fields_on_update() {
    User targetUser = new User("old@test.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param = UpdateUserParam.builder().bio("only the bio changes").build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    assertThat(targetUser.getBio()).isEqualTo("only the bio changes");
    assertThat(targetUser.getEmail()).isEqualTo("old@test.com");
    assertThat(targetUser.getUsername()).isEqualTo("old");
    assertThat(targetUser.getPassword()).isEqualTo("old-password");
    assertThat(targetUser.getImage()).isEqualTo("old image");
    verify(userRepository).save(targetUser);
  }

  @Test
  public void should_keep_every_field_when_the_update_param_is_completely_empty() {
    User targetUser = new User("old@test.com", "old", "old-password", "old bio", "old image");

    userService.updateUser(new UpdateUserCommand(targetUser, UpdateUserParam.builder().build()));

    assertThat(targetUser.getEmail()).isEqualTo("old@test.com");
    assertThat(targetUser.getUsername()).isEqualTo("old");
    assertThat(targetUser.getPassword()).isEqualTo("old-password");
    assertThat(targetUser.getBio()).isEqualTo("old bio");
    assertThat(targetUser.getImage()).isEqualTo("old image");
    verify(userRepository).save(targetUser);
  }

  /**
   * {@code io.spring.Util#isEmpty} only rejects null and "", so a blank value overwrites the
   * existing one. Documented here as current behaviour rather than as desired behaviour.
   */
  @Test
  public void should_apply_a_blank_username_because_only_empty_values_are_ignored() {
    User targetUser = new User("old@test.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param = UpdateUserParam.builder().username("   ").build();

    userService.updateUser(new UpdateUserCommand(targetUser, param));

    assertThat(targetUser.getUsername()).isEqualTo("   ");
  }

  /**
   * The service is {@code @Validated}, so the {@code @Valid} parameters are only checked through
   * the Spring proxy; a bare instance applies no validation. The constraints themselves are
   * asserted directly against a validator below.
   */
  @Test
  public void should_not_validate_register_param_when_service_is_not_proxied() {
    User created = userService.createUser(new RegisterParam("not-an-email", "", ""));

    assertThat(created.getEmail()).isEqualTo("not-an-email");
    verify(userRepository).save(created);
  }

  @Test
  public void should_reject_register_param_with_blank_fields() {
    Set<ConstraintViolation<RegisterParam>> violations =
        validator().validate(new RegisterParam("", "", ""));

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("email", "username", "password");
    assertThat(violations)
        .allSatisfy(violation -> assertThat(violation.getMessage()).isEqualTo("can't be empty"));
  }

  @Test
  public void should_reject_register_param_with_an_invalid_email() {
    Set<ConstraintViolation<RegisterParam>> violations =
        validator().validate(new RegisterParam("not-an-email", "john", "123"));

    assertThat(violations).hasSize(1);
    ConstraintViolation<RegisterParam> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
    assertThat(violation.getMessage()).isEqualTo("should be an email");
  }

  @Test
  public void should_reject_register_param_with_an_existing_email_or_username() {
    User existing = new User("john@test.com", "john", "123", "", "");
    when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(existing));
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(existing));

    Set<ConstraintViolation<RegisterParam>> violations =
        validator().validate(new RegisterParam("john@test.com", "john", "123"));

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("email", "username");
  }

  @Test
  public void should_accept_a_valid_register_param() {
    assertThat(validator().validate(new RegisterParam("fresh@test.com", "fresh", "123"))).isEmpty();
  }

  @Test
  public void should_reject_update_command_taking_an_email_owned_by_another_user() {
    User targetUser = new User("old@test.com", "old", "123", "", "");
    when(userRepository.findByEmail("taken@test.com"))
        .thenReturn(Optional.of(new User("taken@test.com", "other", "123", "", "")));

    Set<ConstraintViolation<UpdateUserCommand>> violations =
        validator()
            .validate(
                new UpdateUserCommand(
                    targetUser, UpdateUserParam.builder().email("taken@test.com").build()));

    assertThat(violations).hasSize(1);
    ConstraintViolation<UpdateUserCommand> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
    assertThat(violation.getMessage()).isEqualTo("email already exist");
  }

  @Test
  public void should_accept_update_command_keeping_the_current_user_own_email_and_username() {
    User targetUser = new User("old@test.com", "old", "123", "", "");
    when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("old")).thenReturn(Optional.of(targetUser));

    Set<ConstraintViolation<UpdateUserCommand>> violations =
        validator()
            .validate(
                new UpdateUserCommand(
                    targetUser,
                    UpdateUserParam.builder().email("old@test.com").username("old").build()));

    assertThat(violations).isEmpty();
  }

  /**
   * Builds a validator whose constraint validators are created by Spring, so that the duplicated
   * email/username constraints get the mocked {@link UserRepository} injected.
   */
  private Validator validator() {
    validationContext = new AnnotationConfigApplicationContext();
    validationContext.getBeanFactory().registerSingleton("userRepository", userRepository);
    validationContext.refresh();
    LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
    validatorFactoryBean.setApplicationContext(validationContext);
    validatorFactoryBean.afterPropertiesSet();
    return validatorFactoryBean;
  }
}
