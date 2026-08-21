package io.spring.api;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.application.data.UserWithToken;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.infrastructure.service.PendoTrackService;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/user")
@AllArgsConstructor
public class CurrentUserApi {

  private UserQueryService userQueryService;
  private UserService userService;
  private PendoTrackService pendoTrackService;

  @GetMapping
  public ResponseEntity currentUser(
      @AuthenticationPrincipal User currentUser,
      @RequestHeader(value = "Authorization") String authorization) {
    UserData userData = userQueryService.findById(currentUser.getId()).get();
    return ResponseEntity.ok(
        userResponse(new UserWithToken(userData, authorization.split(" ")[1])));
  }

  @PutMapping
  public ResponseEntity updateProfile(
      @AuthenticationPrincipal User currentUser,
      @RequestHeader("Authorization") String token,
      @Valid @RequestBody UpdateUserParam updateUserParam) {

    userService.updateUser(new UpdateUserCommand(currentUser, updateUserParam));
    UserData userData = userQueryService.findById(currentUser.getId()).get();

    Map<String, Object> trackProps = new HashMap<>();
    trackProps.put("userId", currentUser.getId());
    trackProps.put(
        "emailChanged",
        updateUserParam.getEmail() != null && !updateUserParam.getEmail().isEmpty());
    trackProps.put(
        "usernameChanged",
        updateUserParam.getUsername() != null && !updateUserParam.getUsername().isEmpty());
    trackProps.put(
        "bioChanged", updateUserParam.getBio() != null && !updateUserParam.getBio().isEmpty());
    trackProps.put(
        "imageChanged",
        updateUserParam.getImage() != null && !updateUserParam.getImage().isEmpty());
    trackProps.put(
        "passwordChanged",
        updateUserParam.getPassword() != null && !updateUserParam.getPassword().isEmpty());
    pendoTrackService.track("user_profile_updated", currentUser.getId(), trackProps);

    return ResponseEntity.ok(userResponse(new UserWithToken(userData, token.split(" ")[1])));
  }

  private Map<String, Object> userResponse(UserWithToken userWithToken) {
    return new HashMap<String, Object>() {
      {
        put("user", userWithToken);
      }
    };
  }
}
