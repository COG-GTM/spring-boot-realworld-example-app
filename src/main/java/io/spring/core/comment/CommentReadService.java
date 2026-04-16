package io.spring.core.comment;

import io.spring.application.CursorPageParameter;
import io.spring.application.data.CommentData;
import java.util.List;
import org.joda.time.DateTime;

public interface CommentReadService {
  CommentData findById(String id);

  List<CommentData> findByArticleId(String articleId);

  List<CommentData> findByArticleIdWithCursor(String articleId, CursorPageParameter<DateTime> page);
}
