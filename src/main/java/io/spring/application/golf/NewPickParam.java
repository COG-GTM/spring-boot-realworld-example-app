package io.spring.application.golf;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("pick")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewPickParam {
  @NotBlank(message = "can't be empty")
  private String tournamentId;

  @NotBlank(message = "can't be empty")
  private String golferId;
}
