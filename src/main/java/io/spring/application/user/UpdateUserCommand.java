package io.spring.application.user;

import io.spring.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@UpdateUserValidator
public class UpdateUserCommand {

  private User targetUser;
  private UpdateUserParam param;
}
