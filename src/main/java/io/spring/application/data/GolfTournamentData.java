package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GolfTournamentData {
  private String id;
  @JsonIgnore private String contestId;
  private String name;
  private String course;
  private DateTime startsAt;
  private DateTime endsAt;
  @JsonIgnore private boolean hasResults;
  private TournamentStatus status;
}
