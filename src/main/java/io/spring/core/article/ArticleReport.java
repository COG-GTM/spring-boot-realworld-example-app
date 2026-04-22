package io.spring.core.article;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class ArticleReport {
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_UPHELD = "UPHELD";
  public static final String STATUS_DISMISSED = "DISMISSED";

  public static final String REASON_SPAM = "SPAM";
  public static final String REASON_HARASSMENT = "HARASSMENT";
  public static final String REASON_ILLEGAL = "ILLEGAL";
  public static final String REASON_OTHER = "OTHER";

  private String id;
  private String articleId;
  private String reporterId;
  private String reason;
  private String reporterComment;
  private String status;
  private String moderatorId;
  private String moderatorNote;
  private DateTime createdAt;
  private DateTime resolvedAt;

  public ArticleReport(String articleId, String reporterId, String reason, String reporterComment) {
    this.id = UUID.randomUUID().toString();
    this.articleId = articleId;
    this.reporterId = reporterId;
    this.reason = reason;
    this.reporterComment = reporterComment;
    this.status = STATUS_PENDING;
    this.createdAt = new DateTime();
  }

  public boolean isPending() {
    return STATUS_PENDING.equals(status);
  }

  public boolean isResolved() {
    return !isPending();
  }

  public void resolve(String status, String moderatorId, String moderatorNote) {
    this.status = status;
    this.moderatorId = moderatorId;
    this.moderatorNote = moderatorNote;
    this.resolvedAt = new DateTime();
  }

  public static boolean isValidReason(String reason) {
    return REASON_SPAM.equals(reason)
        || REASON_HARASSMENT.equals(reason)
        || REASON_ILLEGAL.equals(reason)
        || REASON_OTHER.equals(reason);
  }

  public static boolean isValidResolutionAction(String action) {
    return STATUS_UPHELD.equals(action) || STATUS_DISMISSED.equals(action);
  }
}
