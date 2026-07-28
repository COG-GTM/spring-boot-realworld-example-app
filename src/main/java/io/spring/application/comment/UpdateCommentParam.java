package io.spring.application.comment;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("comment")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentParam {
  @NotBlank(message = "can't be empty")
  private String body;
}
