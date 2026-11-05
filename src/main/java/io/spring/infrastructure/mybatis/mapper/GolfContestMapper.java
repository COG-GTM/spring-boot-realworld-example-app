package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.golf.GolfContest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GolfContestMapper {
  void insert(@Param("contest") GolfContest contest);

  void update(@Param("contest") GolfContest contest);

  GolfContest findById(@Param("id") String id);

  List<GolfContest> findAll();
}
