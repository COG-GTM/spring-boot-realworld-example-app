package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReportData {
  private String id;
  private String articleSlug;
  private String reason;
  private String comment;
  private String status;

  @JsonProperty("reporter")
  private ReportActor reporter;

  @JsonProperty("moderator")
  private ReportActor moderator;

  private String moderatorNote;
  private DateTime createdAt;
  private DateTime resolvedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReportActor {
    private String username;
    private String image;
  }
}
