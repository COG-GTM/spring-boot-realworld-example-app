package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GolfStandingData {
  private int rank;
  @JsonIgnore private String userId;
  private String username;
  private String image;
  private long totalPrizeMoney;
  private int picksMade;
  private List<GolfStandingPickData> picks = new ArrayList<>();
}
