package io.spring.application.user;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserParam {

  @Builder.Default
  @Email(message = "should be an email")
  @Size(max = 255)
  private String email = "";

  @Builder.Default
  @Size(max = 72)
  private String password = "";

  @Builder.Default
  @Size(max = 20)
  private String username = "";

  @Builder.Default
  @Size(max = 65535)
  private String bio = "";

  @Builder.Default
  @Size(max = 512)
  private String image = "";
}
