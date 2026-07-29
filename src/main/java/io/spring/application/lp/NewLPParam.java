package io.spring.application.lp;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.core.lp.Stage;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("lp")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewLPParam {
  @NotBlank(message = "can't be empty")
  private String name;

  @NotBlank(message = "can't be empty")
  private String company;

  @Email(message = "should be an email")
  @NotBlank(message = "can't be empty")
  private String email;

  private Stage stage;
}
