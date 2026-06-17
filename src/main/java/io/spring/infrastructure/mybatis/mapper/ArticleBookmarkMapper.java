package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.bookmark.ArticleBookmark;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleBookmarkMapper {
  ArticleBookmark find(@Param("articleId") String articleId, @Param("userId") String userId);

  void insert(@Param("bookmark") ArticleBookmark bookmark);

  void delete(@Param("bookmark") ArticleBookmark bookmark);

  List<String> findByUserId(
      @Param("userId") String userId, @Param("offset") int offset, @Param("limit") int limit);

  int countByUserId(@Param("userId") String userId);
}
