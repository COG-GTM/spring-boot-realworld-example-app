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
import io.spring.core.user.RefreshToken;
import io.spring.core.user.RefreshTokenRepository;
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
import org.joda.time.DateTime;
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
  private RefreshTokenRepository refreshTokenRepository;

  @RequestMapping(path = "/users", method = POST)
  public ResponseEntity createUser(@Valid @RequestBody RegisterParam registerParam) {
    User user = userService.createUser(registerParam);
    UserData userData = userQueryService.findById(user.getId()).get();
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    saveRefreshToken(user.getId(), refreshToken);
    return ResponseEntity.status(201)
        .body(userResponse(new UserWithToken(userData, accessToken, refreshToken)));
  }

  @RequestMapping(path = "/users/login", method = POST)
  public ResponseEntity userLogin(@Valid @RequestBody LoginParam loginParam) {
    Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
    if (optional.isPresent()
        && passwordEncoder.matches(loginParam.getPassword(), optional.get().getPassword())) {
      User user = optional.get();
      UserData userData = userQueryService.findById(user.getId()).get();
      String accessToken = jwtService.generateAccessToken(user);
      String refreshToken = jwtService.generateRefreshToken(user);
      saveRefreshToken(user.getId(), refreshToken);
      return ResponseEntity.ok(
          userResponse(new UserWithToken(userData, accessToken, refreshToken)));
    } else {
      throw new InvalidAuthenticationException();
    }
  }

  @RequestMapping(path = "/api/auth/refresh", method = POST)
  public ResponseEntity refreshToken(@Valid @RequestBody RefreshTokenParam refreshTokenParam) {
    String token = refreshTokenParam.getRefreshToken();

    Optional<String> userIdOpt = jwtService.validateRefreshToken(token);
    if (!userIdOpt.isPresent()) {
      throw new InvalidAuthenticationException();
    }

    Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
    if (!refreshTokenOpt.isPresent()) {
      throw new InvalidAuthenticationException();
    }

    RefreshToken refreshTokenEntity = refreshTokenOpt.get();

    if (refreshTokenEntity.getExpiresAt().isBeforeNow()) {
      throw new InvalidAuthenticationException();
    }

    String userId = userIdOpt.get();
    Optional<User> userOpt = userRepository.findById(userId);
    if (!userOpt.isPresent()) {
      throw new InvalidAuthenticationException();
    }

    User user = userOpt.get();

    refreshTokenRepository.revokeByToken(token);

    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);
    saveRefreshToken(userId, newRefreshToken);

    UserData userData = userQueryService.findById(userId).get();
    return ResponseEntity.ok(
        userResponse(new UserWithToken(userData, newAccessToken, newRefreshToken)));
  }

  @RequestMapping(path = "/api/auth/logout", method = POST)
  public ResponseEntity logout(@Valid @RequestBody RefreshTokenParam refreshTokenParam) {
    String token = refreshTokenParam.getRefreshToken();

    refreshTokenRepository.revokeByToken(token);

    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("message", "Logged out successfully");
          }
        });
  }

  private void saveRefreshToken(String userId, String refreshToken) {
    DateTime expiresAt = new DateTime().plusSeconds(604800);
    RefreshToken refreshTokenEntity = new RefreshToken(userId, refreshToken, expiresAt);
    refreshTokenRepository.save(refreshTokenEntity);
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
@JsonRootName("refreshToken")
@NoArgsConstructor
class RefreshTokenParam {
  @NotBlank(message = "can't be empty")
  private String refreshToken;
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
