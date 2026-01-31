package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;


@SpringBootTest(classes = {DgsAutoConfiguration.class, UserMutation.class})
public class UserMutationTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean UserRepository userRepository;

  @MockBean PasswordEncoder passwordEncoder;

  @MockBean UserService userService;

  @Test
  public void testCreateUser() {
    when(userService.createUser(any())).thenReturn(user);

    String mutation = "mutation { createUser(input: { email: \"test@example.com\", username: \"testuser\", password: \"password\" }) { ... on UserPayload { user { email username } } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(userService).createUser(any());
  }

  @Test
  public void testLogin() {
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("password"), eq(user.getPassword()))).thenReturn(true);

    String mutation = "mutation { login(email: \"test@example.com\", password: \"password\") { user { email username } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
  }

  @Test
  public void testLoginWithInvalidPassword() {
    when(userRepository.findByEmail(eq("test@example.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("wrongpassword"), eq(user.getPassword()))).thenReturn(false);

    String mutation = "mutation { login(email: \"test@example.com\", password: \"wrongpassword\") { user { email } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login");
    });
  }

  @Test
  public void testLoginWithNonExistentUser() {
    when(userRepository.findByEmail(eq("nonexistent@example.com"))).thenReturn(Optional.empty());

    String mutation = "mutation { login(email: \"nonexistent@example.com\", password: \"password\") { user { email } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.login");
    });
  }

  @Test
  
  public void testUpdateUser() {
    String mutation = "mutation { updateUser(changes: { email: \"newemail@example.com\", username: \"newusername\" }) { user { email username } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
  }

  @Test
  
  public void testUpdateUserWithAllFields() {
    String mutation = "mutation { updateUser(changes: { email: \"newemail@example.com\", username: \"newusername\", password: \"newpassword\", bio: \"new bio\", image: \"new image\" }) { user { email username } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(userService).updateUser(any());
  }
}
