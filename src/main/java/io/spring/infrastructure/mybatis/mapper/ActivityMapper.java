package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.lp.Activity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityMapper {
  void insert(@Param("activity") Activity activity);

  Activity findById(@Param("id") String id);

  List<Activity> findByLpId(@Param("lpId") String lpId);

  void delete(@Param("id") String id);
}
