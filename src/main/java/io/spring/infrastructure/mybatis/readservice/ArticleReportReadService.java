package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.Page;
import io.spring.application.data.ArticleReportData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleReportReadService {
  ArticleReportData findById(@Param("id") String id);

  List<ArticleReportData> findByStatus(@Param("status") String status, @Param("page") Page page);

  int countByStatus(@Param("status") String status);
}
