package io.spring.application.golf;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One golfer's final result. A null position means the golfer did not finish (e.g. cut). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GolferResultParam {
  @NotBlank(message = "can't be empty")
  private String golferId;

  @Positive(message = "must be 1 or greater")
  private Integer position;

  @NotNull(message = "can't be empty")
  @PositiveOrZero(message = "can't be negative")
  private Long prizeMoney;
}
