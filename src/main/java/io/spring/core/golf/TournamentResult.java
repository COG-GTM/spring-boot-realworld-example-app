package io.spring.core.golf;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"tournamentId", "golferId"})
public class TournamentResult {
  private String tournamentId;
  private String golferId;
  private Integer finishPosition;
  private long prizeMoney;

  public TournamentResult(
      String tournamentId, String golferId, Integer finishPosition, long prizeMoney) {
    this.tournamentId = tournamentId;
    this.golferId = golferId;
    this.finishPosition = finishPosition;
    this.prizeMoney = prizeMoney;
  }
}
