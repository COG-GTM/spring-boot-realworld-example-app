package io.spring.api;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.application.user.PasswordResetService;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PasswordResetApi {
  private PasswordResetService passwordResetService;

  @RequestMapping(path = "/users/password-reset", method = POST)
  public ResponseEntity requestPasswordReset(
      @Valid @RequestBody PasswordResetRequestParam requestParam) {
    Map<String, Object> body = new HashMap<>();
    passwordResetService
        .requestReset(requestParam.getEmail())
        .ifPresent(token -> body.put("token", token.getToken()));
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("passwordReset", body);
          }
        });
  }

  @RequestMapping(path = "/users/password-reset", method = PUT)
  public ResponseEntity resetPassword(@Valid @RequestBody PasswordResetParam resetParam) {
    passwordResetService.resetPassword(resetParam.getToken(), resetParam.getPassword());
    return ResponseEntity.noContent().build();
  }
}

@Getter
@JsonRootName("passwordReset")
@NoArgsConstructor
class PasswordResetRequestParam {
  @NotBlank(message = "can't be empty")
  @Email(message = "should be an email")
  private String email;
}

@Getter
@JsonRootName("passwordReset")
@NoArgsConstructor
class PasswordResetParam {
  @NotBlank(message = "can't be empty")
  private String token;

  @NotBlank(message = "can't be empty")
  private String password;
}
