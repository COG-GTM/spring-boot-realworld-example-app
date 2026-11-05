package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.golf.ContestEntry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContestEntryMapper {
  void insert(@Param("entry") ContestEntry entry);

  ContestEntry find(@Param("contestId") String contestId, @Param("userId") String userId);

  List<ContestEntry> findByContestId(@Param("contestId") String contestId);
}
