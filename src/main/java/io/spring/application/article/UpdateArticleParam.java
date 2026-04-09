package io.spring.application.article;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("article")
public class UpdateArticleParam {
  @Size(max = 255)
  private String title = "";

  @Size(max = 65535)
  private String body = "";

  @Size(max = 255)
  private String description = "";
}
