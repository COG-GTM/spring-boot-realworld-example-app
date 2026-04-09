package io.spring.application.user;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("user")
@AllArgsConstructor
@NoArgsConstructor
public class RegisterParam {
  @NotBlank(message = "can't be empty")
  @Email(message = "should be an email")
  @Size(min = 1, max = 255)
  @DuplicatedEmailConstraint
  private String email;

  @NotBlank(message = "can't be empty")
  @Size(min = 1, max = 20)
  @DuplicatedUsernameConstraint
  private String username;

  @NotBlank(message = "can't be empty")
  @Size(min = 8, max = 72)
  private String password;
}
