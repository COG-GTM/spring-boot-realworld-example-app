package io.spring.application.golf;

import io.spring.core.golf.Tournament;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ValidTournamentResults
public class RecordResultsCommand {
  private Tournament tournament;
  @Valid private TournamentResultsParam param;
}
