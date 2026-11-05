package io.spring.application.golf;

import io.spring.core.golf.Tournament;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ValidTournamentResults
public class RecordResultsCommand {
  private Tournament tournament;
  private TournamentResultsParam param;
}
