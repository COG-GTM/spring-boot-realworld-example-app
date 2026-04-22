package io.spring.application.article;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("resolution")
public class ResolveReportParam {
  @NotBlank(message = "can't be empty")
  private String action = "";

  @Size(max = 500, message = "max 500 characters")
  private String note = "";
}
