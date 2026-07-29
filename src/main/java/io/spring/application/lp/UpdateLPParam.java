package io.spring.application.lp;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("lp")
public class UpdateLPParam {
  private String name = "";

  private String company = "";

  @Email(message = "should be an email")
  private String email = "";
}
