package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GolfPickData {
  private String id;
  @JsonIgnore private String contestId;
  private String tournamentId;
  private String tournamentName;
  private DateTime tournamentStartsAt;
  private DateTime tournamentEndsAt;
  @JsonIgnore private boolean hasResults;
  private TournamentStatus tournamentStatus;
  private GolferData golfer;
  private Integer finishPosition;
  private Long prizeMoney;
  private DateTime createdAt;
}
