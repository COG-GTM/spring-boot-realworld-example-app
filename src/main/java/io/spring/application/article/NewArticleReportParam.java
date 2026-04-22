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
@JsonRootName("report")
public class NewArticleReportParam {
  @NotBlank(message = "can't be empty")
  private String reason = "";

  @Size(max = 500, message = "max 500 characters")
  private String comment = "";
}
