package io.spring.application.data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentResultsData {
  private String tournamentId;
  private String tournamentName;
  private List<GolferResultData> results;
}
