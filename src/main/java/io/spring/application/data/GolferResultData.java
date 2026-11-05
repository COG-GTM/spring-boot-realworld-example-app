package io.spring.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GolferResultData {
  private String golferId;
  private String golferName;
  private Integer position;
  private long prizeMoney;
}
