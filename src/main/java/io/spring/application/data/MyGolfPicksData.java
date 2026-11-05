package io.spring.application.data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyGolfPicksData {
  private String contestId;
  private boolean entered;
  private List<GolfPickData> picks;
  private List<GolferData> availableGolfers;
}
