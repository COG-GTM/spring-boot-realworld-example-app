package io.spring.application.golf;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonRootName("tournamentResults")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentResultsParam {
  @NotEmpty(message = "can't be empty")
  @Valid
  private List<GolferResultParam> results;
}
