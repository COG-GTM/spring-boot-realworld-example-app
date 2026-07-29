package io.spring.application.lp;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("activity")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewActivityParam {
  @NotBlank(message = "can't be empty")
  private String type;

  private String notes = "";
}
