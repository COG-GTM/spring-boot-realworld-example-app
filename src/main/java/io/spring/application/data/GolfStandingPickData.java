package io.spring.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GolfStandingPickData {
  private String tournamentId;
  private String tournamentName;
  private String golferId;
  private String golferName;
  private Long prizeMoney;
}
