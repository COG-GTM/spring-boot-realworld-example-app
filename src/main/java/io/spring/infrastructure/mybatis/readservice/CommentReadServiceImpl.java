package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.data.CommentData;
import io.spring.core.comment.CommentReadService;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

@Service
public class CommentReadServiceImpl implements CommentReadService {
  private final MyBatisCommentReadServiceMapper mapper;

  public CommentReadServiceImpl(MyBatisCommentReadServiceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public CommentData findById(String id) {
    return mapper.findById(id);
  }

  @Override
  public List<CommentData> findByArticleId(String articleId) {
    return mapper.findByArticleId(articleId);
  }

  @Override
  public List<CommentData> findByArticleIdWithCursor(
      String articleId, CursorPageParameter<DateTime> page) {
    return mapper.findByArticleIdWithCursor(articleId, page);
  }
}
