package io.spring.application.lp;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.core.lp.Stage;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("lp")
public class UpdateStageParam {
  @NotNull(message = "can't be empty")
  private Stage stage;
}
