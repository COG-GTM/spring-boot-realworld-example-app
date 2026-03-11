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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UsersApi {
  private static final Logger log = LoggerFactory.getLogger(UsersApi.class);

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
    long loginStartTime = System.currentTimeMillis();
    log.info("[LOGIN_PERF] Login attempt started for email: {}", loginParam.getEmail());

    long dbQueryStartTime = System.currentTimeMillis();
    Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
    long dbQueryEndTime = System.currentTimeMillis();
    log.info(
        "[LOGIN_PERF] findByEmail query completed in {}ms", (dbQueryEndTime - dbQueryStartTime));

    if (optional.isPresent()) {
      long bcryptStartTime = System.currentTimeMillis();
      boolean passwordMatches =
          passwordEncoder.matches(loginParam.getPassword(), optional.get().getPassword());
      long bcryptEndTime = System.currentTimeMillis();
      log.info(
          "[LOGIN_PERF] BCrypt password verification completed in {}ms",
          (bcryptEndTime - bcryptStartTime));

      if (passwordMatches) {
        User user = optional.get();
        UserData userData =
            new UserData(
                user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
        log.info("[LOGIN_PERF] UserData created from User object (no second DB query needed)");

        long totalTime = System.currentTimeMillis() - loginStartTime;
        log.info("[LOGIN_PERF] Login successful - total time: {}ms", totalTime);

        return ResponseEntity.ok(userResponse(new UserWithToken(userData, jwtService.toToken(user))));
      }
    }

    long totalTime = System.currentTimeMillis() - loginStartTime;
    log.info("[LOGIN_PERF] Login failed - total time: {}ms", totalTime);
    throw new InvalidAuthenticationException();
  }

  private Map<String, Object> userResponse(UserWithToken userWithToken) {
    return new HashMap<String, Object>() {
      {
        put("user", userWithToken);
      }
    };
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
