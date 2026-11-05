package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.GolfContestData;
import io.spring.application.data.GolfStandingData;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GolfStandingsReadService {
  GolfContestData findContestById(@Param("contestId") String contestId);

  List<GolfStandingData> findStandings(@Param("contestId") String contestId);
}
