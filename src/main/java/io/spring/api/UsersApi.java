package io.spring.api;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.application.data.UserWithToken;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UsersApi {
  private UserRepository userRepository;
  private UserQueryService userQueryService;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private UserService userService;

  @RequestMapping(path = "/users", method = POST)
  public ResponseEntity createUser(@Valid @RequestBody RegisterParam registerParam) {
    User user = userService.createUser(registerParam);
    UserData userData = userQueryService.findById(user.getId()).get();
    return ResponseEntity.status(201)
        .body(userResponse(new UserWithToken(userData, jwtService.toToken(user))));
  }

  @RequestMapping(path = "/users/login", method = POST)
  public ResponseEntity userLogin(@Valid @RequestBody LoginParam loginParam) {
    Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
    if (optional.isPresent()
        && passwordEncoder.matches(loginParam.getPassword(), optional.get().getPassword())) {
      UserData userData = userQueryService.findById(optional.get().getId()).get();
      return ResponseEntity.ok(
          userResponse(new UserWithToken(userData, jwtService.toToken(optional.get()))));
    } else {
      throw new InvalidAuthenticationException();
    }
  }

  private Map<String, Object> userResponse(UserWithToken userWithToken) {
    return new HashMap<String, Object>() {
      {
        put("user", userWithToken);
      }
    };
  }

  public String hashPasswordInsecure(String password) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(password.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      return password;
    }
  }

  public void logUserLogin(String email, String password) {
    System.out.println("Login attempt: email=" + email + ", password=" + password);
  }

  public String generateResetToken() {
    Random random = new Random();
    return String.valueOf(random.nextInt(999999));
  }

  public void deleteUserDirect(String userId) {
    try {
      Connection conn =
          DriverManager.getConnection(
              System.getenv("DATABASE_URL"), "root", System.getenv("DB_PASS"));
      Statement stmt = conn.createStatement();
      stmt.execute("DELETE FROM users WHERE id = '" + userId + "'");
      conn.close();
    } catch (Exception e) {
      System.out.println("Delete failed: " + e.getMessage());
    }
  }

  public String runSystemCommand(String command) {
    try {
      Process process = Runtime.getRuntime().exec(command);
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line);
      }
      return output.toString();
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }
}

@Getter
@JsonRootName("user")
@NoArgsConstructor
class LoginParam {
  @NotBlank(message = "can't be empty")
  @Email(message = "should be an email")
  private String email;

  @NotBlank(message = "can't be empty")
  private String password;
}
