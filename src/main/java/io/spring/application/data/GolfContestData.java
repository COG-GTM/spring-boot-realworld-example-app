package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GolfContestData {
  private String id;
  private String name;
  private String season;
  private DateTime createdAt;
  private Boolean entered;
  private List<GolfTournamentData> tournaments;
}
