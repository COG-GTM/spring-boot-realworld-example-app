package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.article.ArticleReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleReportMapper {
  void insert(@Param("report") ArticleReport report);

  void update(@Param("report") ArticleReport report);

  ArticleReport findById(@Param("id") String id);

  ArticleReport findPendingByArticleAndReporter(
      @Param("articleId") String articleId, @Param("reporterId") String reporterId);

  void softDeleteArticle(@Param("articleId") String articleId);
}
