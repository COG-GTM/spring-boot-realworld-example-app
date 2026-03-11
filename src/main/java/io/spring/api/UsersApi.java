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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersApi {
  private UserRepository userRepository;
  private UserQueryService userQueryService;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private UserService userService;
  private RefreshTokenRepository refreshTokenRepository;
  private int refreshTokenExpirationDays;

  public UsersApi(
      UserRepository userRepository,
      UserQueryService userQueryService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      UserService userService,
      RefreshTokenRepository refreshTokenRepository,
      @Value("${jwt.refreshTokenExpirationDays:7}") int refreshTokenExpirationDays) {
    this.userRepository = userRepository;
    this.userQueryService = userQueryService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.userService = userService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenExpirationDays = refreshTokenExpirationDays;
  }

  @RequestMapping(path = "/users", method = POST)
  public ResponseEntity createUser(@Valid @RequestBody RegisterParam registerParam) {
    User user = userService.createUser(registerParam);
    UserData userData = userQueryService.findById(user.getId()).get();
    RefreshToken refreshToken = new RefreshToken(user.getId(), refreshTokenExpirationDays);
    refreshTokenRepository.save(refreshToken);
    return ResponseEntity.status(201)
        .body(
            userResponse(
                new UserWithToken(userData, jwtService.toToken(user), refreshToken.getToken())));
  }

  @RequestMapping(path = "/users/login", method = POST)
  public ResponseEntity userLogin(@Valid @RequestBody LoginParam loginParam) {
    Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
    if (optional.isPresent()
        && passwordEncoder.matches(loginParam.getPassword(), optional.get().getPassword())) {
      User user = optional.get();
      UserData userData = userQueryService.findById(user.getId()).get();
      RefreshToken refreshToken = new RefreshToken(user.getId(), refreshTokenExpirationDays);
      refreshTokenRepository.save(refreshToken);
      return ResponseEntity.ok(
          userResponse(
              new UserWithToken(userData, jwtService.toToken(user), refreshToken.getToken())));
    } else {
      throw new InvalidAuthenticationException();
    }
  }

  @RequestMapping(path = "/users/token/refresh", method = POST)
  public ResponseEntity refreshToken(@Valid @RequestBody RefreshTokenParam refreshTokenParam) {
    Optional<RefreshToken> optionalToken =
        refreshTokenRepository.findByToken(refreshTokenParam.getRefreshToken());
    if (optionalToken.isPresent() && optionalToken.get().isValid()) {
      RefreshToken existingToken = optionalToken.get();
      // Revoke old token (rotation)
      existingToken.revoke();
      refreshTokenRepository.revokeAllByUserId(existingToken.getUserId());

      Optional<User> optionalUser = userRepository.findById(existingToken.getUserId());
      if (optionalUser.isPresent()) {
        User user = optionalUser.get();
        UserData userData = userQueryService.findById(user.getId()).get();
        RefreshToken newRefreshToken = new RefreshToken(user.getId(), refreshTokenExpirationDays);
        refreshTokenRepository.save(newRefreshToken);
        return ResponseEntity.ok(
            userResponse(
                new UserWithToken(
                    userData, jwtService.toToken(user), newRefreshToken.getToken())));
      }
    }
    throw new InvalidAuthenticationException();
  }

  @RequestMapping(path = "/users/logout", method = POST)
  public ResponseEntity logout(@AuthenticationPrincipal User user) {
    if (user != null) {
      refreshTokenRepository.revokeAllByUserId(user.getId());
    }
    return ResponseEntity.ok().build();
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

@Getter
@NoArgsConstructor
class RefreshTokenParam {
  @NotBlank(message = "can't be empty")
  private String refreshToken;
}
